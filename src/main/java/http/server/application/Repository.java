package http.server.application;

import http.server.error.ErrorFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.Iterator;
import java.util.List;

public class Repository implements AutoCloseable {
    private static final Logger logger = LogManager.getLogger(Repository.class);
    private final String databaseUrl;
    private final String userDatabase;
    private final String passwordDatabase;
    private final Connection connection;

    public Repository(String databaseUrl, String userDatabase, String passwordDatabase) {
        this.databaseUrl = databaseUrl;
        this.userDatabase = userDatabase;
        this.passwordDatabase = passwordDatabase;

        try {
            connection = getConnection();
        } catch (SQLException e) {
            logger.error("Не удалось подключиться к Postgres");
            throw new RuntimeException(e);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(databaseUrl, userDatabase, passwordDatabase);
    }

    int saveUserTable(Visit visit, Connection connection) throws SQLException {
        long visitId = visit.getId();
        if (visitId == -1) {
            try (PreparedStatement ps = connection.prepareStatement(Constants.SAVE_NEW_VISIT_QUERY)) {
                ps.setString(1, visit.getFio());
                ps.setString(2, visit.getContact());
                ps.setTimestamp(3, Timestamp.valueOf(visit.getStartTime()));
                ps.setTimestamp(4, Timestamp.valueOf(visit.getEndTime()));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        visitId = rs.getInt("id");
                        if (visitId <= 0) {
                            throw new SQLException("Failed to save new vizit " + visit);
//                            ErrorFactory.internalErrorDto("Failed to save new vizit " + visit);
                            //logger.error("Failed to save new vizit " + visit);
                        }
                        visit.setId(visitId);
                    }
                } catch (SQLException e) {
//                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            } catch (SQLException e) {
//                e.printStackTrace();
                throw new SQLException(e);
            }
        } else {
            try (PreparedStatement ps = connection.prepareStatement(Constants.SAVE_EXIST_USER_QUERY)) {
                ps.setString(1, visit.getLogin());
                ps.setString(2, visit.getPassword());
                ps.setString(3, visit.getUsername());
                ps.setInt(4, visitId);
                if (ps.executeUpdate() == 0) {
                    System.out.println("Не удалось сохранить существующего пользователя в бд, id = " +
                            visitId);
                    return -2;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                throw new SQLException(e);
            }
        }
        return visitId;
    }

//    public void saveVisit(Vizit vizit) {
//        try (Connection connection = getConnection()) {
//            connection.setAutoCommit(false);
//            int userId;
//            try {
//                userId = saveUserTable(user, connection);
//            } catch (SQLException e) {
//                e.printStackTrace();
//                connection.rollback();
//                throw new RuntimeException(e);
//            }
//
//            try (PreparedStatement ps = connection.prepareStatement(Constants.SAVE_USER_ROLE_QUERY)) {
//                List<Role> roles = readUserRoles(user);
//                Iterator<Role> iteratorRole = user.getRolesIterator();
//                while (iteratorRole.hasNext()) {
//                    Role role = iteratorRole.next();
//                    if (roles != null && roles.contains(role)) {
//                        continue;
//                    }
//                    ps.setInt(1, userId);
//                    ps.setInt(2, role.getId());
//                    if (ps.executeUpdate() == 0) {
//                        System.out.println("Не удалось сохранить роль пользователя в бд, id = " + userId);
//                        return;
//                    }
//                }
//            } catch (SQLException e) {
//                e.printStackTrace();
//                connection.rollback();
//                throw new RuntimeException(e);
//            }
//            connection.commit();
//        } catch (SQLException e) {
//            e.printStackTrace();
//            throw new RuntimeException(e);
//        }
//    }

    @Override
    public void close() throws Exception {
        if (connection != null) {
            connection.close();
            logger.info("Соединение с Postgres закрыто");
        }
    }
}
