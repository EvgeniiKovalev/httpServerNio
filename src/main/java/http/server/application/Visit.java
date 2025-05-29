package http.server.application;

import com.google.gson.annotations.Expose;
import http.server.error.AppException;
import http.server.error.ErrorFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class Visit {
    @Expose()
    private int id;
    @Expose()
    private String fio;
    @Expose()
    private String contact;
    @Expose()
    private LocalDateTime startTime;
    @Expose()
    private LocalDateTime endTime;
    @Expose(serialize = false)
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    public Visit() {
    }

    public Visit(String fio, String contact, LocalDateTime startTime, LocalDateTime endTime) {
        this.id = -1;
        this.fio = fio;
        this.contact = contact;
        this.startTime = startTime;
        this.endTime = endTime;

    }

    public boolean validatePeriod (Repository repository) throws AppException {
        //1
        if (!startTime.isBefore(endTime))
            throw ErrorFactory.badRequest("startStime <= endTime", "INCORRECT_REQUEST_PERIOD");

        //2
        long hoursBetween = ChronoUnit.HOURS.between(startTime, endTime);
        if (hoursBetween < 1 || hoursBetween > 2)
            throw ErrorFactory.badRequest("period cannot be more than 2 hours and less than 1 hour",
                    "INCORRECT_REQUEST_PERIOD");

        //3     startTime > minStartTime and startTime < maxStartTime
        LocalDateTime minStartTime = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).plusHours(2);
        LocalDateTime maxStartTime = minStartTime.plusDays(5);
        if (minStartTime.isAfter(startTime) || maxStartTime.isBefore(startTime))
            throw ErrorFactory.badRequest("Record must be at least 2 hours in advance," +
                    " but not more than 5 days in advance","INCORRECT_REQUEST_PERIOD");

        //4
        boolean overlapsPeriod;
        try {
            overlapsPeriod = repository.checkOverlapsPeriod(startTime, endTime, getId());
        } catch (SQLException e) {
            throw ErrorFactory.internalServerError("Database error", e);
        }
        if (overlapsPeriod)
            throw ErrorFactory.badRequest("Requested period overlaps with periods of other records",
                    "INCORRECT_REQUEST_PERIOD");
        return true;
    }


    public Visit(int id, String fio, String contact, LocalDateTime startTime, LocalDateTime endTime) {
        this(fio, contact, startTime, endTime);
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFio() {
        return fio;
    }

    public void setFio(String fio) {
        this.fio = fio;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Visit visit = (Visit) o;
        return Objects.equals(getId(), visit.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return String.format("Visit{ id = %d, fio = '%s', contact = '%s', startTime = %s, endTime = %s }"
                , id, fio, contact, startTime.format(formatter), endTime.format(formatter));
    }
}