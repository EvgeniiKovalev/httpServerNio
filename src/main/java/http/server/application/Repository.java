package http.server.application;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Repository implements AutoCloseable {
    private static final Logger logger = LogManager.getLogger(Repository.class);
    private static final Gson GSON_INSTANCE = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .excludeFieldsWithoutExposeAnnotation()
            .create();
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

    public static Gson getGson() {
        return GSON_INSTANCE;
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(databaseUrl, userDatabase, passwordDatabase);
    }

    /**
     * Writes a Visit instance to the database
     *
     * @param visit
     * @param connection
     * @return will return a number greater than zero, equal to the id of the recorded table row,
     * otherwise -1 if the record of a new Visit fails, -2 if the record of an existing Visit fails to update
     * @throws SQLException
     */
    int saveVisitTable(Visit visit, Connection connection) throws SQLException {
        int visitId = visit.getId();
        int result = -3;
        if (visitId <= 0) {
            try (PreparedStatement ps = connection.prepareStatement(Constants.INSERT_NEW_VISIT_QUERY)) {
                ps.setString(1, visit.getFio());
                ps.setString(2, visit.getContact());
                ps.setTimestamp(3, Timestamp.valueOf(visit.getStartTime()));
                ps.setTimestamp(4, Timestamp.valueOf(visit.getEndTime()));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        visitId = rs.getInt("id");
                        result = visitId > 0 ? visitId : -1;
                        visit.setId(visitId);
//                        if (visitId <= 0) {
//                            ErrorFactory.internalErrorWithDto("Failed to save new Visit= '" + visit+ "'",
//                                    "description: Failed to save new Visit= '" + visit+ "'");
//                            logger.error("Failed to save new vizit " + visit);
//                        }
                    }
                }
            }
        } else {
            try (PreparedStatement ps = connection.prepareStatement(Constants.UPDATE_EXIST_VISIT_QUERY)) {
                ps.setString(1, visit.getFio());
                ps.setString(2, visit.getContact());
                ps.setTimestamp(3, Timestamp.valueOf(visit.getStartTime()));
                ps.setTimestamp(4, Timestamp.valueOf(visit.getEndTime()));
                ps.setInt(5, visit.getId());
                result = ps.executeUpdate() > 0 ? visitId : -2;
//                if (ps.executeUpdate() == 0) {
//                    ErrorFactory.notFoundErrorWithDto("Visit with id = '" + visit + "' not found",
//                            "description: Visit with id = '" + visit + "' not found");
//                }
            }
        }
        return result;
    }

    public Visit getVisitById(int id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(Constants.SELECT_VISIT_BY_ID_QUERY)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return !rs.next() ? null : new Visit(
                        id,
                        rs.getString("fio"),
                        rs.getString("contact"),
                        rs.getTimestamp("start_time").toLocalDateTime(),
                        rs.getTimestamp("end_time").toLocalDateTime()
                );
            }
        }
    }

    /**
     * Wrap a visit record in a transaction and logs errors
     *
     * @param visit
     * @return Will return true on successful write, otherwise false
     */
    public boolean saveVisit(Visit visit) {
        if (visit == null) {
            logger.error("Empty Visit passed");
            return false;
        }
        String errorMessage;
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            int res = saveVisitTable(visit, connection);
            if (res < 0) {
                connection.rollback();
                switch (res) {
                    case -1 -> errorMessage = "Failed to save new Visit= '" + visit + "'";
                    case -2 -> errorMessage = "Failed to update Visit with id = '" + visit + "' not found";
                    default -> errorMessage = "Unknown result saveVisitTable";
                }
                logger.error(errorMessage);
                return false;
            }
            connection.commit();
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
            return false;
        }
        return true;
    }

    public List<Visit> getAllVisits() throws SQLException{
        try (PreparedStatement ps = connection.prepareStatement(Constants.SELECT_All_VISIT_QUERY)) {
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
