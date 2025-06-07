package http.server.application;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DatabaseSource {
    private static final Logger logger = LogManager.getLogger(DatabaseSource.class);

    private static volatile HikariDataSource instanceHikariDataSource;

    private DatabaseSource() {}

    public static HikariDataSource getDataSource(String dbUrl, String dbUser, String dbPassword, int maxPoolSize){
        if (instanceHikariDataSource != null && !instanceHikariDataSource.isClosed())
            return instanceHikariDataSource;

        if (dbUrl == null || dbUrl.isBlank() || dbUser == null || dbPassword == null)
            throw new IllegalArgumentException("DB URL, user and password must not be null or empty");

        Object lock = new Object();
        synchronized (lock) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(dbUrl);
            config.setUsername(dbUser);
            config.setPassword(dbPassword);

            config.setMaximumPoolSize(maxPoolSize);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(300000);
            config.setMaxLifetime(600000);
            config.setLeakDetectionThreshold(5000);
            config.setPoolName("MyHikariPool");
            instanceHikariDataSource = new HikariDataSource(config);
        }
        return instanceHikariDataSource;
    }

    public static synchronized void closePool() {
        if (instanceHikariDataSource != null && !instanceHikariDataSource.isClosed()) {
            try {
                instanceHikariDataSource.close();
            } catch (Exception e) {
                logger.error("Error closing HikariCP", e);
            }
            logger.info("HikariCP closed");
        }
    }
}
