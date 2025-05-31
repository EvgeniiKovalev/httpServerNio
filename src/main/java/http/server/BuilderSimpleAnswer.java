package http.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BuilderSimpleAnswer {
    private static final Logger logger = LogManager.getLogger(BuilderSimpleAnswer.class);

    public static String headerBody(int statusCode, String errorCode, String body, String contentType) {
        return String.format("%s%s",
                header(statusCode, errorCode, contentType),
                (body == null ? "" :
                        (contentType.equals("text/html") || contentType == null) ? String.format("<html><body><h1>%s</h1></body></html>", body) : body));
    }

    public static String header(int statusCode, String errorCode, String contentType) {

        return String.format("HTTP/1.1 %d %s\r\nContent-Type: %s\r\n\r\n",
                statusCode,
                (errorCode != null ? errorCode :
                        switch (statusCode) {
                            case 200 -> "OK";
                            case 201 -> "Created";
                            case 204 -> "No Content";
                            case 400 -> "Bad Request";
                            case 404 -> "Not Found";
                            default -> "Not Found";
                        }),
                (contentType != null ? contentType : "text/html")
        );
    }

}
