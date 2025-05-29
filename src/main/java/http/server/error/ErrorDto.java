package http.server.error;
import http.server.processors.ErrorProcessor;

import java.io.IOException;
import java.time.LocalDateTime;

public class ErrorDto implements AutoCloseable {
    private String methodRaw;
    private String uri;
    private final HttpErrorType errorType;
    private final String errorCode;
    private final String description;
    private final String datetime;
    private boolean isClosed;

    public String getMethodRaw() {
        return methodRaw == null ? "" : methodRaw;
    }

    public String getUri() {
        return uri == null ? "" : uri;
    }

    protected ErrorDto(HttpErrorType errorType, String description) {
        this.errorType = errorType;
        this.description = description;
        this.errorCode = null;
        this.datetime = LocalDateTime.now().toString();
    }

    protected ErrorDto(HttpErrorType errorType, String errorCode, String description) {
        this.errorType = errorType;
        this.description = description;
        this.errorCode = errorCode;
        this.datetime = LocalDateTime.now().toString();
    }

    protected ErrorDto(AppException appException) {
        this.errorType = appException.getErrorType();
        this.description = appException.getMessage();
        this.errorCode = appException.getErrorCode();
        this.methodRaw = appException.getMethodRaw();
        this.uri = appException.getUri();
        this.datetime = LocalDateTime.now().toString();
    }


    public String getErrorCode() {
        return (errorCode == null || errorCode.isEmpty()) ? errorType.getErrorCode() : errorCode ;
    }

    public String getRoutingKey() {
        return ErrorProcessor.class.getSimpleName();
    }

    public int getStatusCode() {
        return errorType.getStatusCode();
    }

    public String getDescription() {
        return description;
    }

    @Override
    public void close() throws IOException {
        if (isClosed) return;
        isClosed = true;
    }


}