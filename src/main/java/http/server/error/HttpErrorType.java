package http.server.error;

public enum HttpErrorType {
    BAD_REQUEST(400, "BAD_REQUEST") {
        @Override
        public String customErrorCode(String errorCode) {
            return errorCode;
        }
    },
    INTERNAL_SERVER_ERROR(500, "INTERNAL_SERVER_ERROR"),
    NOT_FOUND(404, "NOT_FOUND");

    private final int statusCode;
    private final String errorCode;

    HttpErrorType(int statusCode, String errorCode) {
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public String customErrorCode(String errorCode) {
        return getErrorCode();
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
