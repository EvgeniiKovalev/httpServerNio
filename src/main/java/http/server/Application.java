package http.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;


public class Application {
    private static final int EXIT_WITH_ERROR_READ_CONFIG = 1;
    private static final int EXIT_WITH_ERROR_RUN_SERVER = 2;
    private static final Logger logger = LogManager.getLogger(Application.class);

    public static void main(String[] args) {
        ServerConfig serverConfig = null;
        try {
            serverConfig = new ServerConfig();
            logger.debug("server configuration loaded successfully");
        } catch (Exception e) {
            logger.fatal("Failed to load server configuration: {}", e);
            System.exit(EXIT_WITH_ERROR_READ_CONFIG);
        }

        try (var httpServer = new HttpServer(serverConfig)) {
            logger.debug("HTTP server created successfully");
            httpServer.run();
        } catch (Exception e) {
            logger.fatal("Error while running server ",  e);
            System.exit(EXIT_WITH_ERROR_RUN_SERVER);
        }
    }
}
