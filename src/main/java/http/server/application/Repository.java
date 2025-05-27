package http.server.application;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    int saveVisitTable(Visit visit, Connection connection) throws SQLException {
        int visitId = visit.getId();
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
            try (PreparedStatement ps = connection.prepareStatement(Constants.SAVE_EXIST_VISIT_QUERY)) {
                ps.setString(1, visit.getFio());
                ps.setString(2, visit.getContact());
                ps.setTimestamp(3, Timestamp.valueOf(visit.getStartTime()));
                ps.setTimestamp(4, Timestamp.valueOf(visit.getEndTime()));
                ps.setInt(5, visitId);
                if (ps.executeUpdate() == 0) {
                    throw new SQLException("Failed to save existing vizit, id = " + visitId);
                }
            } catch (SQLException e) {
                e.printStackTrace();
                throw new SQLException(e);
            }
        }
        return visitId;
    }

    public Visit getVisitById(int id) throws SQLException{
        try (PreparedStatement ps = connection.prepareStatement(Constants.VISIT_BY_ID_QUERY)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return !rs.next() ? null : new Visit(
                        id,
                        rs.getString("fio"),
                        rs.getString("contact"),
                        rs.getTimestamp("start_time").toLocalDateTime(),
                        rs.getTimestamp("end_time").toLocalDateTime()
                );
            } catch (SQLException ex) {
                ex.printStackTrace();
                throw new RuntimeException(ex);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    public void saveVisit(Visit visit) {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try {
                int Id = saveVisitTable(visit, connection);
            } catch (SQLException e) {
                e.printStackTrace();
                connection.rollback();
                throw new RuntimeException(e);
            }
            connection.commit();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public List<Visit> getAllVisits() {
        try (PreparedStatement ps = connection.prepareStatement(Constants.All_VISIT_QUERY)) {
            List<Visit> result = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String fio = rs.getString("fio");
                    String contact = rs.getString("contact");
                    LocalDateTime startTime = rs.getTimestamp("start_time").toLocalDateTime();
                    LocalDateTime endTime = rs.getTimestamp("end_time").toLocalDateTime();
                    result.add(new Visit(id, fio, contact, startTime, endTime));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws Exception {
        if (connection != null) {
            connection.close();
            logger.debug("Соединение с Postgres закрыто");
        }
    }
}
