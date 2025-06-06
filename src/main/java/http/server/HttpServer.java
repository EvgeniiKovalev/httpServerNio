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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class HttpServer implements AutoCloseable {
    private static final Logger logger = LogManager.getLogger(HttpServer.class);
    private static RequestRouter requestRouter;
    private final String DATABASE_URL;
    private final String USER_DATABASE;
    private final String PASSWORD_DATABASE;
    private final String HOST;
    private final int PORT;
    private final int BUFFER_SIZE;
    private final int MAX_HTTP_REQUEST_SIZE;
    private final int MAX_HTTP_ANSWER_SIZE;
    private final int TIMEOUT_INPUT_DATA;
    private final int NUM_THREAD;
    private final Map<Integer, Selector> workerSelectors = new ConcurrentHashMap<>();

    private ServerSocketChannel serverChannel;
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
        BUFFER_SIZE = Integer.parseInt(serverConfig.getBufferSize());
        MAX_HTTP_REQUEST_SIZE = Integer.parseInt(serverConfig.getMaxHttpRequestSize());
        MAX_HTTP_ANSWER_SIZE = Integer.parseInt(serverConfig.getMaxHttpAnswerSize());
        TIMEOUT_INPUT_DATA = Integer.parseInt(serverConfig.getTimeoutInputData());
        NUM_THREAD = Integer.parseInt(serverConfig.getNumThread());

        Repository repository = new Repository(DATABASE_URL, USER_DATABASE, PASSWORD_DATABASE);
        requestRouter = new RequestRouter(repository);
    }

    private void initialize() throws IOException {
        logger.trace("Initializing server");
        serverChannel = ServerSocketChannel.open();
        serverChannel.setOption(StandardSocketOptions.SO_RCVBUF, BUFFER_SIZE);
        serverChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        serverChannel.bind(new InetSocketAddress(HOST, PORT));
        serverChannel.configureBlocking(false);
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                logger.info("Received shutdown signal (Ctrl+C)");
                this.close();
            } catch (IOException e) {
                logger.error("Error during shutdown", e);
            }
        }));
    }

    public void run() throws Exception {
        initialize();
        this.isRunning = true;
        addShutdownHook();
        CountDownLatch latch = new CountDownLatch(NUM_THREAD);
        Selector selector;
        for (int workerId = 0; workerId <= NUM_THREAD; workerId++) {
            selector = Selector.open();
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            final Selector finalSelector = selector;
            final Integer finalWorkerId = workerId;
            Thread.startVirtualThread(() -> handlerThread(finalSelector, finalWorkerId, latch));
        }
        logger.info("Server started on {}:{} (started number threads: {})", HOST, PORT, workerSelectors.size());
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Server interrupted", e);
        }
    }

    private void handlerThread(Selector selector, Integer workerId, CountDownLatch latch) {
        if (workerId == null || selector == null) return;

        try {
            workerSelectors.put(workerId, selector);
            while (isRunning) {
                if (selector.select(TIMEOUT_INPUT_DATA) == 0) continue;
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();
                    if (key == null || !key.isValid()) continue;

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
            if (!isRunning) {
                safeCloseSelector(workerId, selector);
                workerSelectors.remove(workerId);
            }
        }
    }

    private void accept(SelectionKey key) {
        if (workerSelectors.size() > NUM_THREAD) {
            logger.warn("Connection rejected (limit reached)");
            return;
        }
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = null;

        try {
            clientChannel = serverChannel.accept();
            if (clientChannel == null) return;

            clientChannel.configureBlocking(false);
            clientChannel.setOption(StandardSocketOptions.SO_RCVBUF, BUFFER_SIZE);
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

    /**
     * Handles writing data to the client channel.
     *
     * <p>Key operations:
     * <ol>
     *   <li>Validates the request context (closes connection if missing)</li>
     *   <li>Enforces maximum write attempts (3) to prevent hangs</li>
     *   <li>Writes response data from the output buffer</li>
     *   <li>Re-registers for write ops if data remains (backpressure handling)</li>
     *   <li>Ensures proper connection cleanup in all cases</li>
     * </ol>
     *
     * <p>Flow control:
     * <ul>
     *   <li>Negative write = immediate close (connection error)</li>
     *   <li>Partial write = re-register for more writes</li>
     *   <li>Complete write = close connection</li>
     * </ul>
     *
     * @param key The selection key containing channel and context
     * @throws IOException for low-level I/O errors
     * @throws IllegalStateException if context structure is invalid
     */
    private void write(SelectionKey key) throws Exception {
        SocketChannel channel = (SocketChannel) key.channel();
        Context context = (Context) key.attachment();
        if (context == null) {
            logger.warn("Missing context for write operation");
            safeClose(channel, key);
            return;
        }
        if (context.incAndGetWriteAttempts() > 3) {
            logger.warn("Write attempts exceeded");
            safeClose(channel, key);
            return;
        }

        ByteBuffer out = context.getRequestAnswer().getByteBuffer();
        int written = channel.write(out);
        if (written < 0) {
            safeClose(channel, key);
            return;
        }

        if (out.hasRemaining()) {
            if (written == 0) logger.debug("channel is not ready to receive data");
            reRegisteredKey(key, SelectionKey.OP_WRITE);
            return;
        }
        safeClose(channel, key);
    }

    private void reRegisteredKey(SelectionKey key, int Operation) {
        if (key != null && key.isValid()) {
            key.interestOps(Operation);
            key.selector().wakeup();
        }
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

    private void safeCloseSelector(Integer workerId, Selector selector) {
        if (!isRunning && selector != null && selector.isOpen()) {
            try {
                for (SelectionKey key : selector.keys()) {
                    if (key != null && key.channel() instanceof SocketChannel)
                        safeClose((SocketChannel) key.channel(), key);
                }
                selector.close();
                logger.debug("Selector closed for worker {}", workerId);
            } catch (IOException e) {
                logger.error("Failed close selector in worker {}", workerId, e);
            }
        }
    }

    private void safeClose(SocketChannel channel, SelectionKey key) {
        if (channel == null) return;

        try {
            if (channel.isOpen()) {
                if (channel.isConnected()) {
                    try {
                        channel.shutdownInput();
                    } catch (IOException ignored) {
                    }
                    try {
                        channel.shutdownOutput();
                    } catch (IOException ignored) {
                    }
                }
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

    private void cleanupServerChannelKey() {
        for (Selector selector : workerSelectors.values()) {
            if (selector == null || !selector.isOpen()) continue;

            for (SelectionKey key : selector.keys()) {
                if (key != null && key.channel() == serverChannel) {
                    key.cancel();
                    break;
                }
            }
        }
    }

    @Override
    public synchronized void close() throws IOException {
        if (!isRunning) return;
        logger.info("Server stopping...");
        System.out.println("Server stopping...");
        isRunning = false;

        workerSelectors.values().forEach(selector -> { if (selector != null) selector.wakeup(); });

        long stopTime = System.currentTimeMillis() + 5000;
        while (!workerSelectors.isEmpty() && System.currentTimeMillis() < stopTime) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        cleanupServerChannelKey();
        if (serverChannel != null) serverChannel.close();

        workerSelectors.forEach(this::safeCloseSelector);
        workerSelectors.clear();
        logger.info("Server stopped");
        System.out.println("Server stopped");
    }
}