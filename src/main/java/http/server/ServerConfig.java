package http.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class ServerConfig {
    //todo добавить server.idleTimeout = 30000 ?
    //todo добавить server.maxThreads= 10 ?
    private final Properties properties;

    public ServerConfig() {
        properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("server.properties")) {
            if (input == null) {
                throw new RuntimeException("Unable to find server.properties");
            }
            properties.load(new InputStreamReader(input, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("error loading server properties", e);
        }
    }

    public String getPasswordDatabase() {
        return properties.getProperty("server.passwordDatabase", "1234");
    }

    public String getUserDatabase() {
        return properties.getProperty("server.userDatabase", "admin");
    }

    public String getDatabaseUrl() {
        return properties.getProperty("server.database-url", "jdbc:postgresql://localhost:5433/learn");
    }

    public String getPort() {
        return properties.getProperty("server.port", "8080");
    }

    public String getHost() {
        return properties.getProperty("server.host", "0.0.0.0");
    }

    public String getMaxConnections() {
        return properties.getProperty("server.max-connections", "1000");
    }

    public String getMaxHttpHeaderSizeKb() {
        return properties.getProperty("server.max-http-header-size-kb", "8");
    }

    public String getMaxHttpRequestSizeMb() {
        return properties.getProperty("server.max-http-request-size-mb", "2");
    }

    public String getMaxHttpAnswerSizeMb() {
        return properties.getProperty("server.max-http-answer-size-mb", "10");
    }

    public String getBufferSizeKb() {
        return properties.getProperty("server.buffer-size-kb", "8");
    }

    public String getMaxEmptyRead() {
        return properties.getProperty("server.max-empty-read", "3");
    }

}