package http.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class ServerConfig {
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

    public String getMaxHttpRequestSize() {
        return properties.getProperty("server.max-http-request-size", "100");
    }

    public String getMaxHttpAnswerSize() {
        return properties.getProperty("server.max-http-answer-size", "100");
    }

    public String getBufferSize() {
        return properties.getProperty("server.buffer-size", "8192");
    }

    public String getTimeoutInputData() {
        return properties.getProperty("server.timeout_input_data", "10000");
    }

    public String getNumThread() {
        return properties.getProperty("server.num_thread", "1");
    }

    public String getUseVirtualThread() {
        return properties.getProperty("server.use_virtual_thread", "false");
    }

}