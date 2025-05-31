package http.server;

import http.server.application.Repository;
import http.server.error.ErrorDto;
import http.server.error.ErrorFactory;
import http.server.error.HttpErrorType;
import http.server.parser.ParsingResult;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpServer implements AutoCloseable {
    private static final Logger logger = LogManager.getLogger(HttpServer.class);
    private static RequestRouter requestRouter;
    public final String DATABASE_URL;
    public final String USER_DATABASE;
    public final String PASSWORD_DATABASE;
    private final String HOST;
    private final int PORT;
    private final int MAX_CONNECTIONS;
    public final int BUFFER_SIZE;
    public final int MAX_HTTP_REQUEST_SIZE;
    public final int MAX_HTTP_ANSWER_SIZE;
    private ServerSocketChannel serverChannel;
    private Selector selector;
    private volatile boolean isRunning;


    public HttpServer(ServerConfig serverConfig) {
        if (serverConfig == null) {
            throw new IllegalArgumentException("serverConfig must not be null");
        }
        ServerValidator.validate(serverConfig);
        DATABASE_URL = serverConfig.getDatabaseUrl();
        USER_DATABASE = serverConfig.getUserDatabase();
        PASSWORD_DATABASE = serverConfig.getPasswordDatabase();
        HOST = serverConfig.getHost();
        PORT = Integer.parseInt(serverConfig.getPort());
        MAX_CONNECTIONS = Integer.parseInt(serverConfig.getMaxConnections());
        BUFFER_SIZE = Integer.parseInt(serverConfig.getBufferSize());
        MAX_HTTP_REQUEST_SIZE = Integer.parseInt(serverConfig.getMaxHttpRequestSize());
        MAX_HTTP_ANSWER_SIZE = Integer.parseInt(serverConfig.getMaxHttpAnswerSize());

        Repository repository = new Repository(DATABASE_URL, USER_DATABASE, PASSWORD_DATABASE);
        requestRouter = new RequestRouter(repository);
    }


    private void initialize() throws IOException {
        logger.trace("initialize");
        serverChannel = ServerSocketChannel.open();
        serverChannel.setOption(StandardSocketOptions.SO_RCVBUF, 1024);
        serverChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        serverChannel.socket().bind(new InetSocketAddress(HOST, PORT));
        serverChannel.configureBlocking(false);
        selector = Selector.open();
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        this.isRunning = true;
        logger.info("server started, host: {},  порт: {}, макс. кол-во соединений: {}", HOST, PORT, MAX_CONNECTIONS);
        this.pool = Executors.newFixedThreadPool(10);
    }

    public void run() throws Exception {
        logger.trace("run");
        initialize();
        while (isRunning && serverChannel.isOpen()) {
            int readyChannels = selector.select(); // blocking
            if (readyChannels == 0) {
                continue;
            }
            logger.trace("readyChannels: {} key(s)", readyChannels);
            var selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> keysIterator = selectedKeys.iterator();
            while (keysIterator.hasNext()) {
                SelectionKey key = keysIterator.next();
                keysIterator.remove();

                if (!key.isValid()) continue;
                int readyOps = key.readyOps();
                switch (readyOps) {
                    case SelectionKey.OP_ACCEPT -> accept(selector, serverChannel, key);
                    case SelectionKey.OP_READ -> read(key);
                    case SelectionKey.OP_WRITE -> write(key);
                    default -> logger.warn("Unknown type readyOps {}, key : {}", readyOps, key);
                }
            }
            selectedKeys.clear();
        }
        logger.info("server stopped");
    }

    private void accept(Selector selector, ServerSocketChannel serverChannel, SelectionKey key) {
        logger.trace("accept");
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
            logger.trace("New clientChannel connected: {}", clientChannel.getRemoteAddress());
        } catch (IOException e) {
            logger.error("Accept error ", e);
            closeConnection(clientChannel, key);
        }
    }

    private void closeConnection(SocketChannel clientChannel, SelectionKey key) {
        logger.trace("closeConnection {}", key);
        try {
            boolean ex;
            ex = (key != null && key.isValid());
            if (!ex) return;
            try {
                Object attachment = key.attachment();
                if (attachment instanceof AutoCloseable) {
                    ((AutoCloseable) attachment).close();
                }
            } catch (Exception e) {
                logger.error("Error closing attachment", e);
            }
            if (clientChannel != null && clientChannel.isOpen()) clientChannel.close();
        } catch (IOException e) {
            logger.error("error closing connection ", e);
        } finally {
            key.attach(null);
            key.cancel();
        }
    }

    private void read(SelectionKey key) {
        logger.trace("read");
        if (key == null || !key.isValid()) return;
        SocketChannel clientChannel = (SocketChannel) key.channel();
        if (!clientChannel.isOpen()) return;

        try {
            ByteBuffer inputByteBuffer = ByteBuffer.allocate(BUFFER_SIZE);

            int bytesRead = clientChannel.read(inputByteBuffer);
            if (bytesRead > 0) {
                Context context = new Context();
                context.setInputBuffer(inputByteBuffer);
                context.incLengthRequest(bytesRead);
                ParsingResult parsingResult;
                if (context.getLengthRequest() <= MAX_HTTP_REQUEST_SIZE) {
                    parsingResult = RequestParser.parseToResult(inputByteBuffer);
                } else {
                    ErrorDto errorDto = ErrorFactory.createErrorDto(
                            HttpErrorType.BAD_REQUEST,
                            "ERROR_REQUEST_TOO_LARGE",
                            "Request size exceeds allowed limit (" + MAX_HTTP_REQUEST_SIZE + " bytes)"
                    );
                    parsingResult = ParsingResult.error(errorDto);
                }
                context.setParsingResult(parsingResult);
                requestRouter.route(context, clientChannel, inputByteBuffer);
                if (context.getRequestAnswer() == null)
                    logger.debug("------------------------------context.getRequestAnswer() == null");
                key.attach(context);
                key.interestOps(SelectionKey.OP_WRITE);
                selector.wakeup();
                if (key.attachment() == null)
                    logger.debug("1 key.attachment() == null");
            } else if (bytesRead == -1) {
                closeConnection(clientChannel, key);
                logger.trace("Connection closed by clientChannel");
                if (key.attachment() == null) logger.debug(" 2 key.attachment() == null");
            } else {
                closeConnection(clientChannel, key);
                logger.warn("Invalid bytesRead: {}", bytesRead);
            }
        } catch (Exception e) {
            logger.error("Read error ", e);
            closeConnection(clientChannel, key);
        }
    }

    private void write(SelectionKey key) {
        logger.trace("write");
        if (key == null || !key.isValid()) return;
        SocketChannel clientChannel = (SocketChannel) key.channel();
        if (!clientChannel.isOpen()) {
            closeConnection(clientChannel, key);
            return;
        }
        try {
            Context context = (Context) key.attachment();
            if (context == null) {
                logger.trace("context == null");
                return;
            }
            RequestAnswer requestAnswer = context.getRequestAnswer();
            ByteBuffer out = requestAnswer.getByteBuffer();

            if ((key == null || !key.isValid() || !clientChannel.isOpen() || out.position() >= out.limit())) {
                logger.trace("skipped ");
                return;
            }
            clientChannel.write(out);
        } catch (Exception e) {
            logger.error("Write outer error", e);
        } finally {
            closeConnection(clientChannel, key);
        }
    }

    @Override
    public void close() throws IOException {
        logger.trace("close");
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