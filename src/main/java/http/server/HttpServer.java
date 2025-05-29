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
        logger.info("server started, host: {},  порт: {}", HOST, PORT);
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
            Iterator<SelectionKey> keysIterator = selector.selectedKeys().iterator();
            while (keysIterator.hasNext()) {
                SelectionKey key = keysIterator.next();
                keysIterator.remove();
                if (key.isAcceptable()) {
                    accept(selector, serverChannel, key);
                } else if (key.isReadable()) {
                    read(key);
                } else if (key.isWritable()) {
                    write(key);
                } else {
                    logger.trace("Unknown type key: {}", key.toString());
                }
            }
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
            logger.debug("New clientChannel connected: {}", clientChannel.getRemoteAddress());
        } catch (IOException e) {
            logger.error("Accept error: {}", e.getMessage(), e);
            closeConnection(clientChannel, key);
        }
    }

    private void closeConnection(SocketChannel clientChannel, SelectionKey key) {
        try {
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
            if (clientChannel != null && clientChannel.isOpen()) clientChannel.close();
        } catch (IOException e) {
            logger.error("error closing connection: {}", e.getMessage(), e);
        }
    }

    private void read(SelectionKey key) {
        logger.trace("read");
        if (key == null || !key.isValid()) return;

        SocketChannel clientChannel = (SocketChannel) key.channel();
        try {
            ByteBuffer inputByteBuffer = ByteBuffer.allocate(BUFFER_SIZE);

            int bytesRead = clientChannel.read(inputByteBuffer);
            if (bytesRead > 0) {
                Context context = new Context();
                context.setInputBuffer(inputByteBuffer);
                context.incLengthRequest(bytesRead);
                if (context.getLengthRequest() <= MAX_HTTP_REQUEST_SIZE) {
                    context.setParsingResult(RequestParser.parseToResult(inputByteBuffer));
                } else {
                    ErrorDto errorDto = ErrorFactory.createErrorDto(
                            HttpErrorType.BAD_REQUEST,
                            "ERROR_REQUEST_TOO_LARGE",
                            "Request size exceeds allowed limit (" + MAX_HTTP_REQUEST_SIZE + " bytes)"
                    );
                    context.setParsingResult(ParsingResult.error(errorDto));
                }
                requestRouter.route(context, clientChannel, inputByteBuffer);
                key.attach(context);
                key.interestOps(SelectionKey.OP_WRITE);
                selector.wakeup();
            } else if (bytesRead == -1) {
                closeConnection(clientChannel, key);
                logger.trace("Connection closed by clientChannel");
            } else {
                closeConnection(clientChannel, key);
                logger.warn("Invalid bytesRead: {}", bytesRead);
            }
        } catch (Exception e) {
            logger.error("Read error: {}", e.getMessage(), e);
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
            RequestAnswer requestAnswer = context.getRequestAnswer();
            ByteBuffer outputByteBuffer = requestAnswer.getByteBuffer();
            clientChannel.write(outputByteBuffer);
        } catch (Exception e) {
            logger.error("Write outer error: {}", e.getMessage(), e);
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