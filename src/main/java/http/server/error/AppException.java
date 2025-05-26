package http.server.error;

public class AppException extends RuntimeException {
    private final HttpErrorType errorType;

    public AppException(String message, HttpErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    protected AppException(String message, HttpErrorType errorType, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }

    public static AppException create(String message, HttpErrorType errorType) {
        return new AppException(message, errorType, null);
    }

    public static AppException create(String message, HttpErrorType errorType, Throwable cause) {
        return new AppException(message, errorType, cause);
    }

    public HttpErrorType getErrorType() {
        return errorType;
    }

    @Override
    public String getMessage() {
        return "HTTP " + errorType.getStatusCode() + ": " + super.getMessage();
    }

    @Override
    public String toString() {
        return "AppException{" +
                "errorType=" + errorType +
                ", message='" + super.getMessage() + '\'' +
                '}';
    }
}