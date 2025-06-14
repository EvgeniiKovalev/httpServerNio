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

        String bufferSize = serverConfig.getBufferSize();
        if (bufferSize == null || bufferSize.trim().isEmpty()) {
            throw new IllegalArgumentException("Buffer size must be not empty");
        }
        try {
            int bufferSizeInt = Integer.parseInt(bufferSize);
            if (bufferSizeInt < 1 || bufferSizeInt > 8192)
                throw new IllegalArgumentException("Buffer size must be >= 1 and <= 8192");
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Buffer size must be >= 1 and <= 8192");
        }

        String maxHttpRequestSize = serverConfig.getMaxHttpRequestSize();
        if (maxHttpRequestSize == null || maxHttpRequestSize.trim().isEmpty()) {
            throw new IllegalArgumentException("Max http request size must be not empty");
        }
        try {
            int maxHttpRequestSizeInt = Integer.parseInt(maxHttpRequestSize);
            if (maxHttpRequestSizeInt < 1 || maxHttpRequestSizeInt > 8192)
                throw new IllegalArgumentException("Max http request size must be >= 1  and <= 8192");
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Max http request size(MB) must be >= 1 and <= 8192");
        }

        String maxHttpAnswerSize = serverConfig.getMaxHttpAnswerSize();
        if (maxHttpAnswerSize == null || maxHttpAnswerSize.trim().isEmpty()) {
            throw new IllegalArgumentException("Max http answer size must be not empty");
        }
        try {
            int maxHttpAnswerSizeInt = Integer.parseInt(maxHttpAnswerSize);
            if (maxHttpAnswerSizeInt < 1 || maxHttpAnswerSizeInt > 8192)
                throw new IllegalArgumentException("Max http answer size must be >= 1 and <= 8192");
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Max http answer size must be >= 1 and <= 8192");
        }

        String timeoutInputData = serverConfig.getTimeoutInputData();
        if (timeoutInputData == null || timeoutInputData.trim().isEmpty()) {
            throw new IllegalArgumentException("Timeout input data must be not empty");
        }
        try {
            int timeoutInputDataInt = Integer.parseInt(timeoutInputData);
            if (timeoutInputDataInt < 1000 || timeoutInputDataInt > 100000)
                throw new IllegalArgumentException("Timeout input data must be >= 1000 and <= 100000");
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Timeout input data must be >= 1000 and <= 100000");
        }

        String numThread = serverConfig.getNumThread();
        if (numThread == null || numThread.trim().isEmpty()) {
            throw new IllegalArgumentException("Timeout input data must be not empty");
        }
        try {
            int numThreadInt = Integer.parseInt(numThread);
            if (numThreadInt < 1 || numThreadInt > 1000)
                throw new IllegalArgumentException("Number of thread must be >= 1 and <= 1000");
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Number of thread must be >= 1 and <= 1000");
        }

        String useVirtualThread = serverConfig.getUseVirtualThread();
        if (useVirtualThread == null || useVirtualThread.trim().isEmpty()) {
            throw new IllegalArgumentException("Use virtual threads must be not empty");
        }
        if (!useVirtualThread.equals("false") && !useVirtualThread.equals("true")) {
            throw new IllegalArgumentException("Use virtual threads must be true or false");
        }

        String maxPoolSize = serverConfig.getMaxPoolSize();
        if (maxPoolSize == null || maxPoolSize.trim().isEmpty()) {
            throw new IllegalArgumentException("maxPoolSize must be not empty");
        }
        try {
            int intMaxPoolSize = Integer.parseInt(maxPoolSize);
            if (intMaxPoolSize < 1 || intMaxPoolSize > 50)
                throw new IllegalArgumentException("Maximum pool size must be >= 1 and <= 50");
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Maximum pool size must be >= 1 and <= 50");
        }
    }
}
