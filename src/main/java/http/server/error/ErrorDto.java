package http.server.error;

import java.io.IOException;
import java.time.LocalDateTime;

public class ErrorDto implements AutoCloseable {
    private final HttpErrorType errorType;
    private final String description;
    private final String datetime;
    private boolean isClosed;

    ErrorDto(HttpErrorType errorType, String description) {
        this.errorType = errorType;
        this.description = description;
        this.datetime = LocalDateTime.now().toString();
    }

    public String getRoutingKey() {
        return "ErrorRoutingKey";
    }

    public HttpErrorType getErrorType() {
        return errorType;
    }

    public String getDescription() {
        return description;
    }

    public String getDatetime() {
        return datetime;
    }

    @Override
    public void close() throws IOException {
        if (isClosed) return;
        isClosed = true;
    }
}