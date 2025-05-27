package http.server.application;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class Visit {
    private int id;
    private String fio;
    private String contact;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public Visit() {
    }

    public Visit(int id, String fio, String contact, LocalDateTime startTime, LocalDateTime endTime) {
        this.id = id;
        this.fio = fio;
        this.contact = contact;
        this.startTime = startTime;
        this.endTime = endTime;
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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
        return String.format("Visit{ id = %d, fio = '%s', contact = '%s', startTime = %s, endTime = %s }"
                , id, fio, contact, startTime.format(formatter), endTime.format(formatter));
    }
}