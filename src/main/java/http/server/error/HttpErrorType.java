package http.server.error;

public enum HttpErrorType {
    BAD_REQUEST(400, "BAD_REQUEST") {
        @Override
        public String resolveCode(String subCode) {
            return "BR_" + subCode;
        }
    },
    INTERNAL_ERROR(500, "INTERNAL_ERROR"),
    NOT_FOUND(404, "NOT_FOUND");

    private final int statusCode;
    private final String errorCode;

    HttpErrorType(int statusCode, String errorCode) {
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public String resolveCode(String subCode) {
        return getErrorCode();
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
