package http.server;

import http.server.answer.RequestAnswer;
import http.server.application.Repository;
import http.server.parser.RequestParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

public class HttpServer implements AutoCloseable {
    private static final Logger logger = LogManager.getLogger(HttpServer.class);
    private static RequestRouter requestRouter;
    public final int BUFFER_SIZE_KB;
    public final int MAX_HTTP_HEADER_SIZE_KB;
    public final int MAX_HTTP_REQUEST_SIZE_MB;
    public final int MAX_HTTP_ANSWER_SIZE_MB;
    public final int MAX_EMPTY_READ;
    public final String DATABASE_URL;
    public final String USER_DATABASE;
    public final String PASSWORD_DATABASE;
    private final int PORT;
    private final String HOST;
    private final int MAX_CONNECTIONS;
    private final Map<SocketChannel, Integer> emptyReadCounters = new WeakHashMap<>(); //ConcurrentHashMap
    private final ServerConfig serverConfig;
//    private final Repository repository;
    private ServerSocketChannel serverChannel;
    private Selector selector;
    private volatile boolean isRunning;


    public HttpServer(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
        if (serverConfig == null) {
            throw new IllegalArgumentException("serverConfig must not be null");
        }
        ServerValidator.validate(serverConfig);
        HOST = serverConfig.getHost();
        PORT = Integer.parseInt(serverConfig.getPort());
        DATABASE_URL = serverConfig.getDatabaseUrl();
        USER_DATABASE = serverConfig.getUserDatabase();
        PASSWORD_DATABASE = serverConfig.getPasswordDatabase();
        MAX_CONNECTIONS = Integer.parseInt(serverConfig.getMaxConnections());
        MAX_HTTP_HEADER_SIZE_KB = Integer.parseInt(serverConfig.getMaxHttpHeaderSizeKb());
        BUFFER_SIZE_KB = Integer.parseInt(serverConfig.getBufferSizeKb());
        MAX_HTTP_REQUEST_SIZE_MB = Integer.parseInt(serverConfig.getMaxHttpRequestSizeMb());
        MAX_HTTP_ANSWER_SIZE_MB = Integer.parseInt(serverConfig.getMaxHttpAnswerSizeMb());
        MAX_EMPTY_READ = Integer.parseInt(serverConfig.getMaxEmptyRead());

        Repository repository = new Repository(DATABASE_URL, USER_DATABASE, PASSWORD_DATABASE);
        requestRouter = new RequestRouter(repository);
    }


    private void initialize() throws IOException {
        logger.debug("initialize");


        serverChannel = ServerSocketChannel.open();
        serverChannel.setOption(StandardSocketOptions.SO_RCVBUF, 1024);
        serverChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        serverChannel.socket().bind(new InetSocketAddress(HOST, PORT));
        serverChannel.configureBlocking(false);
        selector = Selector.open();
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        this.isRunning = true;
        logger.info("Server started, host: {},  порт: {}", HOST, PORT);
    }

    public void run() throws Exception {
        logger.debug("run");
        initialize();
        while (isRunning && serverChannel.isOpen()) {
            int selected = selector.select(); // blocking
            if (selected == 0) {
                continue;
            }
            logger.info("selected: {} key(s)", selected);
            Iterator<SelectionKey> keysIterator = selector.selectedKeys().iterator();
            // нужно ли удалять из итератора ключ до его обработки, или оставить как есть - после обработки ?
            while (keysIterator.hasNext()) {
                SelectionKey key = keysIterator.next();
                if (key.isAcceptable()) {
                    accept(selector, serverChannel, key);
                } else if (key.isReadable()) {
                    read(key);
                } else if (key.isWritable()) {
                    write(key);
                } else {
                    logger.debug("Unknown type key: {}", key.toString());
                }
                keysIterator.remove();
                logger.debug("last line of the keysIterator loop");
            }
        }
        logger.info("Server stopped");
    }

    private void accept(Selector selector, ServerSocketChannel serverChannel, SelectionKey key) {
        logger.debug("accept");
        SocketChannel clientChannel = null;
        try {
            clientChannel = serverChannel.accept();
            if (selector.keys().size() >= MAX_CONNECTIONS) {
                logger.warn("Max connections reached (active connection={}, MAX_CONNECTIONS = {}), " +
                        "rejecting new connection", selector.keys().size(), MAX_CONNECTIONS);
                clientChannel.close();
                return;
            }
            if (clientChannel == null) return;
            clientChannel.configureBlocking(false);
            clientChannel.register(selector, SelectionKey.OP_READ);
            logger.debug("New clientChannel connected: {}", clientChannel.getRemoteAddress());
        } catch (IOException e) {
            logger.error("Accept error: {}", e.getMessage(), e);
            closeConnection(clientChannel, key);
        }
    }

    private void closeConnection(SocketChannel clientChannel, SelectionKey key) {
        try {
            if (clientChannel != null && clientChannel.isOpen()) clientChannel.close();
        } catch (IOException e) {
            logger.error("error closing connection: {}", e.getMessage(), e);
        } finally {
            emptyReadCounters.remove(clientChannel);
            if (key != null && key.isValid()) {
                try {
                    Object attachment = key.attachment();
                    if (attachment instanceof AutoCloseable) {
                        ((AutoCloseable) attachment).close();
                    }
                } catch (Exception e) {
                    logger.error("Error closing attachment: {}", e.getMessage(), e);
                }
                key.attach(null);
                key.cancel();
            }
        }
    }

    /**
     * processing a connection in which the clientChannel does not send data
     */
    private void handleEmptyRead(SocketChannel clientChannel, SelectionKey key) {
        int count = emptyReadCounters.getOrDefault(clientChannel, 0) + 1;
        if (count > MAX_EMPTY_READ) {
            logger.warn("Closing idle connection: {}", clientChannel);
            closeConnection(clientChannel, key);
            return;
        }
        emptyReadCounters.put(clientChannel, count);
        logger.debug("Empty read #{} from {}", count, clientChannel.socket().getRemoteSocketAddress());
    }

    private void read(SelectionKey key) {
        logger.debug("read");
        if (key == null || !key.isValid()) return;
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer inputByteBuffer = ByteBuffer.allocate(MAX_HTTP_HEADER_SIZE_KB * 1024);
        Context context = new Context();
        try {
            int bytesRead = clientChannel.read(inputByteBuffer);
            if (bytesRead > 0) {
                context.setParsingResult(RequestParser.parseToResult(inputByteBuffer));
                try {
                    requestRouter.route(context, clientChannel, inputByteBuffer);
                } catch (Exception e) {

                }


//                RequestProcessor requestProcessor = requestRouter.getProcessor(context.getParsingResultRoutingKey());
//                requestProcessor.route(context, clientChannel);

//                if (context.getParsingResult().isError()) {
//                    closeConnection(clientChannel, key);
//                    return;
//                }
                key.attach(context);
                key.interestOps(SelectionKey.OP_WRITE);
            } else if (bytesRead == 0) {
                handleEmptyRead(clientChannel, key);
            } else if (bytesRead == -1) {
                closeConnection(clientChannel, key);
                logger.warn("Connection closed by clientChannel");
            } else {
                closeConnection(clientChannel, key);
                logger.warn("Invalid bytesRead: {}", bytesRead);
            }
        } catch (IOException e) {
            logger.error("Read error: {}", e.getMessage(), e);
            closeConnection(clientChannel, key);
        }
    }

//    private void processClientData(SelectionKey key, ByteBuffer buffer, int bytesRead) {
//        //todo реализовать через интерфейс, чтобы можно было в зависимости от routingKey полиморфно по интерфейсу вызывать
//        //todo нужный метод (это слой бизнес логики)
//        logger.debug("processClientData");
//        buffer.flip();
//        try {
//            String message = StandardCharsets.US_ASCII.decode(buffer).toString();
//            logger.debug("Received bytes: {}, message: {}", bytesRead, message.trim());
//            key.interestOps(SelectionKey.OP_WRITE);
//        } finally {
//            buffer.clear();
//        }
//    }

    private void write(SelectionKey key) {
        logger.debug("write");
        if (key == null || !key.isValid()) return;
        SocketChannel clientChannel = (SocketChannel) key.channel();
        if (!clientChannel.isOpen()) {
            closeConnection(clientChannel, key);
            return;
        }
        try {
            Context context = (Context) key.attachment();
            RequestAnswer requestAnswer = context.getRequestAnswer();
            ByteBuffer byteBuffer = requestAnswer.getByteBuffer();
            clientChannel.write(byteBuffer);
        } catch (Exception e) {
            logger.error("Write outer error: {}", e.getMessage(), e);
        } finally {
            closeConnection(clientChannel, key);
        }
    }

    @Override
    public void close() throws IOException {
        logger.debug("close");
        IOException firstException = null;

        if (selector != null) {
            try {
                selector.wakeup();
                selector.close();
            } catch (IOException e) {
                firstException = e;
            }
        }

        if (serverChannel != null) {
            try {
                serverChannel.close();
            } catch (IOException e) {
                if (firstException != null) {
                    firstException.addSuppressed(e);
                } else {
                    firstException = e;
                }
            }
        }

        if (firstException != null) {
            logger.debug("Resources closed with errors");
            throw firstException;
        }
        logger.debug("All resources closed successfully");
    }
}
