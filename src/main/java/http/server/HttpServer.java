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
import java.nio.channels.*;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;

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
    private final Selector[] workerSelectors = new Selector[4];
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
        logger.trace("Initializing server");
        serverChannel = ServerSocketChannel.open();
        serverChannel.setOption(StandardSocketOptions.SO_RCVBUF, 1024);
        serverChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        serverChannel.bind(new InetSocketAddress(HOST, PORT));
        serverChannel.configureBlocking(false);

        this.isRunning = true;
        logger.info("Server started on {}:{} (max connections: {})", HOST, PORT, MAX_CONNECTIONS);
    }

    public void run() throws Exception {
        initialize();
        CountDownLatch latch = new CountDownLatch(workerSelectors.length);
        for (int i = 0; i < workerSelectors.length; i++) {
            workerSelectors[i] = Selector.open();
            serverChannel.register(workerSelectors[i], SelectionKey.OP_ACCEPT);

            int workerId = i;
            Thread.startVirtualThread(() -> {
                try {
                    while (isRunning) {
                        workerSelectors[workerId].select();
                        Iterator<SelectionKey> keys = workerSelectors[workerId].selectedKeys().iterator();

                        while (keys.hasNext()) {
                            SelectionKey key = keys.next();
                            keys.remove();

                            try {
                                if (key.isAcceptable()) {
                                    accept(key);
                                } else if (key.isReadable()) {
                                    read(key);
                                } else if (key.isWritable()) {
                                    write(key);
                                }
                            } catch (Exception e) {
                                handleOperationError(e, key);
                            }
                        }
                    }
                } catch (IOException e) {
                    handleNetworkError(e, "Worker " + workerId);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
    }

    private void accept(SelectionKey key) {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = null;

        try {
            clientChannel = serverChannel.accept();
            if (clientChannel == null) return;

            clientChannel.configureBlocking(false);
            clientChannel.register(key.selector(), SelectionKey.OP_READ);

        } catch (IOException e) {
            handleNetworkError(e, "Accept", clientChannel);
            safeClose(clientChannel, null);
        }
    }

    private void read(SelectionKey key) throws Exception {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        int bytesRead = channel.read(buffer);

        if (bytesRead > 0) {
            Context context = new Context();
            context.setInputBuffer(buffer);
            context.incLengthRequest(bytesRead);

            if (context.getLengthRequest() > MAX_HTTP_REQUEST_SIZE) {
                ErrorDto error = ErrorFactory.createErrorDto(
                        HttpErrorType.BAD_REQUEST,
                        "REQUEST_TOO_LARGE",
                        "Max size: " + MAX_HTTP_REQUEST_SIZE
                );
                context.setParsingResult(ParsingResult.error(error));
            } else {
                context.setParsingResult(RequestParser.parseToResult(buffer));
            }

            requestRouter.route(context, channel, buffer);
            key.attach(context);
            key.interestOps(SelectionKey.OP_WRITE);
            key.selector().wakeup();
        } else if (bytesRead == -1) {
            logger.debug("Client closed connection");
            safeClose(channel, key);
        }
    }

    private void write(SelectionKey key) throws Exception {
        SocketChannel channel = (SocketChannel) key.channel();
        Context context = (Context) key.attachment();
        if (context == null) {
            logger.warn("Missing context for write operation");
            safeClose(channel, key);
            return;
        }

        ByteBuffer out = context.getRequestAnswer().getByteBuffer();
        channel.write(out);
        safeClose(channel, key);
    }

    private void handleOperationError(Exception e, SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        if (e instanceof IOException) {
            handleNetworkError((IOException) e, "IO operation", channel);
        } else {
            logger.error("Operation processing error", e);
        }
        safeClose(channel, key);
    }

    private void handleNetworkError(IOException e, String operation) {
        handleNetworkError(e, operation, null);
    }

    private void handleNetworkError(IOException e, String operation, SocketChannel channel) {
        String clientInfo = channel != null ? getRemoteAddress(channel) : "unknown";
        if (e instanceof ClosedChannelException) {
            logger.debug("{}: Connection closed by client ({})", operation, clientInfo);
        } else {
            logger.warn("{} failed for {}: {}", operation, clientInfo, e.getMessage());
        }
    }

    private String getRemoteAddress(SocketChannel channel) {
        try {
            return channel.getRemoteAddress().toString();
        } catch (IOException e) {
            return "address_unavailable";
        }
    }

    private void safeClose(SocketChannel channel, SelectionKey key) {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
        } catch (IOException e) {
            logger.debug("Channel close error", e);
        } finally {
            cleanupKey(key);
        }
    }

    private void cleanupKey(SelectionKey key) {
        if (key == null || !key.isValid()) return;

        try {
            Object attachment = key.attachment();
            if (attachment instanceof AutoCloseable) {
                ((AutoCloseable) attachment).close();
            }
        } catch (Exception e) {
            logger.warn("Attachment cleanup failed", e);
        } finally {
            key.attach(null);
            key.cancel();
        }
    }

    @Override
    public void close() throws IOException {
        isRunning = false;

        for (Selector selector : workerSelectors) {
            if (selector != null) {
                selector.wakeup();
                selector.close();
            }
        }

        if (serverChannel != null) {
            serverChannel.close();
        }

        logger.info("Server stopped");
    }
}