package http.server.application;

import com.google.gson.annotations.Expose;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class Visit  {
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
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    public Visit() {
    }

    public Visit(String fio, String contact, LocalDateTime startTime, LocalDateTime endTime) {
        this.fio = fio;
        this.contact = contact;
        this.startTime = startTime;
        this.endTime = endTime;
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