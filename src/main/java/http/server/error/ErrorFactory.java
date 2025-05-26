package http.server.error;

public final class ErrorFactory {

    private ErrorFactory() {
    }

    public static ErrorDto createErrorDto(HttpErrorType errorType, String description) {
        return new ErrorDto(errorType, description);
    }

    public static AppException badRequest(String message) {
        return AppException.create(message, HttpErrorType.BAD_REQUEST);
    }

    public static AppException internalServerError(String message) {
        return AppException.create(message, HttpErrorType.INTERNAL_SERVER_ERROR);

    }

    public static AppException notFoundError(String message) {
        return AppException.create(message, HttpErrorType.NOT_FOUND);
    }

    public static ErrorDto badRequestDto(String description) {
        return new ErrorDto(HttpErrorType.BAD_REQUEST, description);
    }

    public static ErrorDto internalErrorDto(String description) {
        return new ErrorDto(HttpErrorType.INTERNAL_SERVER_ERROR, description);
    }

    public static ErrorDto notFoundErrorDto(String description) {
        return new ErrorDto(HttpErrorType.NOT_FOUND, description);
    }

    public static AppExceptionWithDto badRequestWithDto(String message, String description) {
        return new AppExceptionWithDto(message, HttpErrorType.BAD_REQUEST, badRequestDto(description));
    }

    public static AppExceptionWithDto internalErrorWithDto(String message, String description) {
        return new AppExceptionWithDto(message, HttpErrorType.INTERNAL_SERVER_ERROR, internalErrorDto(description));
    }

    public static AppExceptionWithDto notFoundErrorWithDto(String message, String description) {
        return new AppExceptionWithDto(message, HttpErrorType.NOT_FOUND, notFoundErrorDto(description));
    }

    public static class AppExceptionWithDto extends AppException {
        private final ErrorDto errorDto;

        public AppExceptionWithDto(String message, HttpErrorType errorType, ErrorDto errorDto) {
            super(message, errorType, null);
            this.errorDto = errorDto;
        }
    }

}
