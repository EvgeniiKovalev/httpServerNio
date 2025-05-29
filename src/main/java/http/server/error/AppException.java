package http.server.error;

public class AppException extends RuntimeException {
    private String methodRaw;
    private String uri;
    private final HttpErrorType errorType;
    private final String customErrorCode;


    protected AppException(String message, HttpErrorType errorType) {
        super(message);
        this.errorType = errorType;
        this.customErrorCode = null;
    }

    protected AppException(String message, HttpErrorType errorType, String methodRaw, String uri, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.customErrorCode = null;
        this.methodRaw = methodRaw;
        this.uri = uri;
    }

    protected AppException(String message, HttpErrorType errorType, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.customErrorCode = null;
    }

    protected AppException(String message, HttpErrorType errorType, String customErrorCode, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.customErrorCode = customErrorCode;
    }



    protected static AppException create(String message, HttpErrorType errorType) {
        return new AppException(message, errorType, null);
    }

    protected static AppException create(String message, HttpErrorType errorType, Throwable cause) {
        return new AppException(message, errorType, cause);
    }

    protected static AppException create(String message, HttpErrorType errorType, String customErrorCode, Throwable cause) {
        return new AppException(message, errorType, customErrorCode, cause);
    }

    protected static AppException create(String message, HttpErrorType errorType, String methodRaw, String uri, Throwable cause) {
        return new AppException(message, errorType, methodRaw, uri, cause);
    }



    public String getErrorCode() {
        return (customErrorCode == null || customErrorCode.isEmpty()) ? errorType.getErrorCode() : customErrorCode;
    }

    public String getMethodRaw() {
        return methodRaw;
    }

    public String getUri() {
        return uri;
    }

    protected HttpErrorType getErrorType() {
        return errorType;
    }

    @Override
    public String getMessage() {
        return super.getMessage();
    }

    @Override
    public String toString() {
        return "AppException{" +
                "errorType=" + getErrorType() +
                ", errorCode=" + getErrorCode() +
                ", message='" + getMessage() +
                "'}";
    }
}