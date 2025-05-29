package http.server.error;

public final class ErrorFactory {

    private ErrorFactory() {
    }

    public static ErrorDto createErrorDto(HttpErrorType errorType, String description) {
        return new ErrorDto(errorType, description);
    }

    public static ErrorDto createErrorDto(HttpErrorType errorType, String errorCode, String description) {
        return new ErrorDto(
                errorType,
                errorCode,
                description
        );
    }

    public static ErrorDto createErrorDto(AppException ex) {
        return new ErrorDto(ex);
    }

    public static AppException badRequest(String message) {
        return AppException.create(message, HttpErrorType.BAD_REQUEST);
    }

    public static AppException badRequest(String message, String methodRaw, String uri, Throwable cause) {
        return AppException.create(message, HttpErrorType.BAD_REQUEST, methodRaw, uri, cause);
    }

    public static AppException badRequest(String message, String customErrorCode) {
        return AppException.create(
                message,
                HttpErrorType.BAD_REQUEST,
                HttpErrorType.BAD_REQUEST.customErrorCode(customErrorCode),
                null
        );
    }

    public static AppException internalServerError(String message) {
        return AppException.create(message, HttpErrorType.INTERNAL_SERVER_ERROR);
    }

    public static AppException internalServerError(String message, Throwable cause) {
        return AppException.create(message, HttpErrorType.INTERNAL_SERVER_ERROR, cause);
    }

    public static AppException notFoundError(String message) {
        return AppException.create(message, HttpErrorType.NOT_FOUND);
    }

    public static ErrorDto internalErrorDto(String description) {
        return new ErrorDto(HttpErrorType.INTERNAL_SERVER_ERROR, description);
    }

    public static ErrorDto notFoundErrorDto(String description) {
        return new ErrorDto(HttpErrorType.NOT_FOUND, description);
    }
}
