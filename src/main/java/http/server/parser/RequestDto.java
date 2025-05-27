package http.server.parser;

import http.server.HttpMethod;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class RequestDto implements AutoCloseable {
    private final Map<String, String> parameters = new HashMap<>();
    private HttpMethod method;
    private String uri;
    private String httpVersion;
    private Path filePath;       // Для GetStaticFile
    private boolean isLargeFile; // Нужна ли потоковая передача?
    private Map<String, String> headers = new HashMap<>();
//    private Map<String, String> variables = new HashMap<>();

    private boolean isClosed;
    private int bytesParsed;

    public RequestDto() {
    }

    public RequestDto(HttpMethod method, String uri) {
        this.method = method;
        this.uri = uri;
    }

    public RequestDto(HttpMethod method, String uri, Map<String, String> headers) {
        this(method, uri);
        this.headers = headers;
    }

    public int getBytesParsed() {
        return bytesParsed;
    }

    public void setBytesParsed(int bytesParsed) {
        this.bytesParsed = bytesParsed;
    }

    public void incBytesParsed(int bytesParsed) {
        this.bytesParsed += bytesParsed;
    }

    public String getRoutingKey() {
        return method.toString() + " " + uri;
    }

    public String getValueFromHeader(String name) {
        return (headers == null) ? null : headers.get(name);
    }

    public HttpMethod getMethod() {
        return method;
    }

    public void setMethod(HttpMethod method) {
        this.method = method;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getHttpVersion() {
        return httpVersion;
    }

    public void setHttpVersion(String httpVersion) {
        this.httpVersion = httpVersion;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Iterator<Map.Entry<String, String>> getHeadersIterator() {
        return headers.entrySet().iterator();
    }

    public Iterator<Map.Entry<String, String>> getParametersIterator() {
        return parameters.entrySet().iterator();
    }

    public void addParameter(String name, String value) {
        parameters.put(name, value);
    }

    public String getParameter(String name) {
        return parameters.get(name);
    }

    public void addHeader(String name, String value) {
        headers.put(name, value);
    }

    @Override
    public String toString() {
        return "http.server.parser.RequestDto{" +
                "method=" + method +
                ", uri='" + uri + '\'' +
                ", http version='" + httpVersion + '\'' +
                ", parameters=" + parameters +
                ", headers=" + headers +
                '}';
    }

    @Override
    public void close() throws IOException {
        if (isClosed) return;
        parameters.clear();
        headers.clear();
        filePath = null;
        isClosed = true;
    }
}
