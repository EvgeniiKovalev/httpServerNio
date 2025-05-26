package http.server;

public class ServerValidator {

    public static void validate(ServerConfig serverConfig) {

        if (serverConfig.getHost() == null || serverConfig.getHost().trim().isEmpty()) {
            throw new IllegalArgumentException("Host must not be empty");
        }

        if (serverConfig.getDatabaseUrl() == null || serverConfig.getDatabaseUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("Database_url must not be empty");
        }

        if (serverConfig.getUserDatabase() == null || serverConfig.getUserDatabase().trim().isEmpty()) {
            throw new IllegalArgumentException("User database must not be empty");
        }

        if (serverConfig.getPasswordDatabase() == null || serverConfig.getPasswordDatabase().trim().isEmpty()) {
            throw new IllegalArgumentException("Password database must not be empty");
        }

        String port = serverConfig.getPort();
        if (port == null || port.trim().isEmpty()) {
            throw new IllegalArgumentException("Port must be not empty");
        }
        try {
            int portInt = Integer.parseInt(port);
            if (portInt <= 0 || portInt >= 65535) throw new IllegalArgumentException("Port must be > 0 and < 65535");
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Port must be > 0 and < 65535");
        }

        String maxConnections = serverConfig.getMaxConnections();
        if (maxConnections == null || maxConnections.trim().isEmpty()) {
            throw new IllegalArgumentException("Max connections must be not empty");
        }
        try {
            int maxConnectionsInt = Integer.parseInt(maxConnections);
            if (maxConnectionsInt <= 0 || maxConnectionsInt > 1000)
                throw new IllegalArgumentException("Max connections must be > 0 and <= 1000");
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Max connections must be > 0 and <= 1000");
        }

        String bufferSizeKb = serverConfig.getBufferSizeKb();
        if (bufferSizeKb == null || bufferSizeKb.trim().isEmpty()) {
            throw new IllegalArgumentException("Buffer size must be not empty");
        }
        try {
            int bufferSizeKbInt = Integer.parseInt(bufferSizeKb);
            if (bufferSizeKbInt < 1 || bufferSizeKbInt > 1024)
                throw new IllegalArgumentException("Buffer size(KB) must be >= 1 and <= 1024");
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Buffer size(KB) must be >= 1 and <= 1024");
        }

        String maxHttpHeaderSizeKb = serverConfig.getMaxHttpHeaderSizeKb();
        if (maxHttpHeaderSizeKb == null || maxHttpHeaderSizeKb.trim().isEmpty()) {
            throw new IllegalArgumentException("Max http header size(KB) must be not empty");
        }
        try {
            int maxHttpHeaderSizeKbInt = Integer.parseInt(maxHttpHeaderSizeKb);
            if (maxHttpHeaderSizeKbInt < 8 || maxHttpHeaderSizeKbInt > 16)
                throw new IllegalArgumentException("Max http header size(KB) must be >= 8 and <= 16");
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Max http header size(KB) must be >= 8 and <= 16");
        }

        String maxHttpRequestSizeMb = serverConfig.getMaxHttpRequestSizeMb();
        if (maxHttpRequestSizeMb == null || maxHttpRequestSizeMb.trim().isEmpty()) {
            throw new IllegalArgumentException("Max http request size(MB) must be not empty");
        }
        try {
            int maxHttpRequestSizeMbInt = Integer.parseInt(maxHttpRequestSizeMb);
            if (maxHttpRequestSizeMbInt < 2 || maxHttpRequestSizeMbInt > 10)
                throw new IllegalArgumentException("Max http request size(MB) must be >= 2 and <= 10");
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Max http request size(MB) must be >= 2 and <= 10");
        }

        String maxHttpAnswerSizeMb = serverConfig.getMaxHttpAnswerSizeMb();
        if (maxHttpAnswerSizeMb == null || maxHttpAnswerSizeMb.trim().isEmpty()) {
            throw new IllegalArgumentException("Max http answer size(MB) must be not empty");
        }
        try {
            int maxHttpAnswerSizeMbInt = Integer.parseInt(maxHttpAnswerSizeMb);
            if (maxHttpAnswerSizeMbInt < 2 || maxHttpAnswerSizeMbInt > 10)
                throw new IllegalArgumentException("Max http answer size(MB) must be >= 2 and <= 10");
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Max http answer size(MB) must be >= 2 and <= 10");
        }

        String maxEmptyRead = serverConfig.getMaxEmptyRead();
        if (maxEmptyRead == null || maxEmptyRead.trim().isEmpty()) {
            throw new IllegalArgumentException("Max empty read must be not empty");
        }
        try {
            int maxEmptyReadInt = Integer.parseInt(maxEmptyRead);
            if (maxEmptyReadInt < 1 || maxEmptyReadInt > 10)
                throw new IllegalArgumentException("Max empty read must be >= 1 and <= 10");
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Max empty read must be >= 1 and <= 10");
        }
    }
}
