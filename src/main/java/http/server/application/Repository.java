package http.server.application;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import http.server.error.AppException;
import http.server.error.ErrorFactory;
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

    public boolean checkExistVisitById(int id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(Constants.CHECK_EXIST_VISIT_BY_ID_QUERY)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
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

    public boolean deleteVisitById(int id) throws SQLException {
        if (id <= 0) {
            throw ErrorFactory.internalServerError("id for delete must be > 0");
        }
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(Constants.DELETE_VISIT_BY_ID_QUERY)) {
            connection.setAutoCommit(false);
            ps.setInt(1, id);
            try {
                if (ps.executeUpdate() > 0) {
                    connection.commit();
                    return true;
                }
                connection.rollback();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
            return false;
        }
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

    /**
     * Inserts a new Visit record into the database within a transaction.
     * Sets the generated ID to the Visit object if successful.
     *
     * @param visit the Visit object to persist (non-null)
     * @return true if inserted successfully with valid ID, false otherwise
     * @throws AppException,SQLException if database error occurs or passed empty visit
     */
    public boolean insertVisit (Visit visit) throws AppException,SQLException {
        if (visit == null) {
            throw ErrorFactory.internalServerError("Null Visit passed to insertVisit");
        }
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     Constants.INSERT_NEW_VISIT_QUERY,
                     Statement.RETURN_GENERATED_KEYS)) {
            connection.setAutoCommit(false);
            ps.setString(1, visit.getFio());
            ps.setString(2, visit.getContact());
            ps.setTimestamp(3, Timestamp.valueOf(visit.getStartTime()));
            ps.setTimestamp(4, Timestamp.valueOf(visit.getEndTime()));
            ps.executeUpdate();
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    visit.setId(generatedKeys.getInt(1));
                    connection.commit();
                    return true;
                }
                connection.rollback();
                logger.error("No generated keys after insert for Visit: {}", visit);
            } catch (SQLException e) {
                throw ErrorFactory.internalServerError(e.getMessage(), e);
            }
        }
        return false;
    }

    public boolean checkOverlapsPeriod(LocalDateTime startPeriod, LocalDateTime endPeriod, int excludeId) throws AppException,SQLException {
        if (startPeriod == null || endPeriod == null) {
            throw ErrorFactory.internalServerError("Null startPeriod or null endPeriod passed to checkOverlapsPeriod");
        }

        try (PreparedStatement ps = connection.prepareStatement(Constants.SELECT_VISIT_BY_OVERLAPS_PERIOD_QUERY)) {
            ps.setInt(1, excludeId);
            ps.setTimestamp(2, Timestamp.valueOf(endPeriod));
            ps.setTimestamp(3, Timestamp.valueOf(startPeriod));

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            } catch (SQLException e) {
                throw ErrorFactory.internalServerError(e.getMessage(), e);
            }
        }
    }


    /**
     * Updates existing Visit record into the database within a transaction.
     *
     * @param visit the Visit object to update (non-null)
     * @return true if updated successfully, false otherwise
     * @throws AppException,SQLException if database error occurs or passed empty visit
     */
    public boolean updateVisit (Visit visit) throws AppException,SQLException {
        if (visit == null) {
            throw ErrorFactory.internalServerError("Null Visit passed to updateVisit");
        }


        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     Constants.UPDATE_EXIST_VISIT_QUERY,
                     Statement.RETURN_GENERATED_KEYS)) {
            connection.setAutoCommit(false);
            ps.setString(1, visit.getFio());
            ps.setString(2, visit.getContact());
            ps.setTimestamp(3, Timestamp.valueOf(visit.getStartTime()));
            ps.setTimestamp(4, Timestamp.valueOf(visit.getEndTime()));
            ps.setInt(5, visit.getId());
            ps.executeUpdate();
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    connection.commit();
                    return true;
                }
                connection.rollback();
                logger.error("No generated keys after update for Visit: {}", visit);
            } catch (SQLException e) {
                throw ErrorFactory.internalServerError(e.getMessage(), e);
            }
        }
        return false;
    }


    @Override
    public void close() throws Exception {
        if (connection != null) {
            connection.close();
            logger.debug("Соединение с Postgres закрыто");
        }
    }
}
