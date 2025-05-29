package http.server.parser;

import http.server.HttpMethod;
import http.server.error.AppException;
import http.server.error.ErrorFactory;

import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * The number of bytes read from the buffer is considered equal to InputByteBuffer.position(),
 * since it is read only forward (such an implementation)
 * InputByteBuffer processing does not use:
 * - mark,
 * - rewind,
 * - position() with parameter less than current position plus 1,
 * - more than once flip
 */
public class RequestParser {
    private static final byte CR = (byte) '\r';
    private static final byte LF = (byte) '\n';
    private static final byte AMP = (byte) '&';
    private static final byte EQUALITY = (byte) '=';
    private static final byte SPACE = (byte) ' ';
    private static final byte QUESTION = (byte) '?';
    private static final byte COLON = (byte) ':';
    private static final byte QUOTES = (byte) '"';
    private static final Set<Byte> SET_EXC_FOR_PARAM = Set.of(QUESTION);
    private static final Set<Byte> SET_FOR_URI = Set.of(QUESTION, SPACE, CR, LF);
    private static final Set<Byte> END_LINE_WITH_SPACE = Set.of(SPACE, CR, LF);
    private static final Set<Byte> END_LINE_WITH_ZERO = Set.of(CR, LF, (byte) 0);
    private static final Set<Byte> END_LINE = Set.of(CR, LF);
    private static final Set<Byte> END_PARAM_NAME = Set.of(EQUALITY, AMP, SPACE, CR, LF);
    private static final Set<Byte> END_PARAM_VALUE = Set.of(AMP, SPACE, CR, LF);
    private static final Charset utf8 = StandardCharsets.UTF_8;


    private static final ThreadLocal<StringBuilder> threadLocalStringBuilder = ThreadLocal.withInitial(() -> new StringBuilder(64));

    public static byte parseParameters(RequestDto requestDto, ByteBuffer inputByteBuffer, StringBuilder stringBuilder) {
        byte foundByte = 0;

        while (inputByteBuffer.hasRemaining()) {
            foundByte = parsePart(inputByteBuffer, END_PARAM_NAME, stringBuilder, SET_EXC_FOR_PARAM, "INCORRECT_REQUEST_PARAMETER");
            if (foundByte != EQUALITY) {
                throw ErrorFactory.badRequest("Parameter name not specified in request URI",
                        "INCORRECT_REQUEST_PARAMETER");
            }
            if (stringBuilder.isEmpty()) {
                throw ErrorFactory.badRequest("Parameter name cannot be empty in request URI",
                        "INCORRECT_REQUEST_PARAMETER");
            }
            String name = URLDecoder.decode(stringBuilder.toString(), utf8);
            foundByte = parsePart(inputByteBuffer, END_PARAM_VALUE, stringBuilder);
            if (stringBuilder.isEmpty()) {
                throw ErrorFactory.badRequest("Parameter value with parameter name '" +
                        name + "' cannot be empty in request URI",
                        "INCORRECT_REQUEST_PARAMETER");
            }
            requestDto.addParameter(name,  URLDecoder.decode(stringBuilder.toString(), utf8));
            if (END_LINE_WITH_SPACE.contains(foundByte)) {
                break;
            }
            if (foundByte == SPACE || foundByte == CR || foundByte == LF || foundByte == 0) {
                break;
            }
        }
        return foundByte;
    }

    public static ParsingResult parseToResult(ByteBuffer inputByteBuffer) {
        try {
            return ParsingResult.success(parse(inputByteBuffer));
        } catch (AppException e) {
            return ParsingResult.error(e);
        }
    }

    public static RequestDto parse(ByteBuffer inputByteBuffer) throws AppException {
        if (inputByteBuffer == null) {
            throw ErrorFactory.internalServerError("inputByteBuffer is null");
        }
        inputByteBuffer.flip();
        RequestDto requestDto = new RequestDto();
        StringBuilder stringBuilder = threadLocalStringBuilder.get();
        parseFirstLine(inputByteBuffer, stringBuilder, requestDto);
        parseHeaders(requestDto, inputByteBuffer, stringBuilder);
        requestDto.setBytesParsed(inputByteBuffer.position());
        return requestDto;
    }

    private static void skip(ByteBuffer inputByteBuffer, int n) {
        inputByteBuffer.position(inputByteBuffer.position() + n);
    }

    private static void skipUntil(ByteBuffer inputByteBuffer, byte stopByte) throws AppException {
        while (inputByteBuffer.hasRemaining()) {
            if (inputByteBuffer.get() == stopByte) return;
        }
        throw ErrorFactory.badRequest("Expected character with code '" + stopByte + "' not found",
                "INCORRECT_REQUEST_PARAMETER");
    }

    private static void parseFirstLine(ByteBuffer inputByteBuffer, StringBuilder stringBuilder, RequestDto requestDto) throws AppException {

        // http method
        byte foundByte = parsePart(inputByteBuffer, END_LINE_WITH_SPACE, stringBuilder);
        requestDto.setMethodRaw(stringBuilder.toString());

        if (END_LINE_WITH_ZERO.contains(foundByte)) {
            throw ErrorFactory.badRequest("HTTP request is invalid");
        }

        // uri
        foundByte = parsePart(inputByteBuffer, SET_FOR_URI, stringBuilder);
        if (END_LINE_WITH_ZERO.contains(foundByte)) {
            throw ErrorFactory.badRequest("HTTP version not specified");
        }
        boolean uriCanBeEmpty = (foundByte == QUESTION);
        if (!uriCanBeEmpty && stringBuilder.isEmpty()) {
            throw ErrorFactory.badRequest("URI not specified");
        }
        requestDto.setUri(URLDecoder.decode(stringBuilder.toString(),utf8));
        if (requestDto.getUri().length() >= 1024) {
            throw ErrorFactory.badRequest("URI Too Long");
        }

        //This is here to have the URI read for an unknown HTTP method
        try {
            requestDto.setMethod(HttpMethod.valueOf(requestDto.getMethodRaw()));
        } catch (IllegalArgumentException ex) {
            throw ErrorFactory.badRequest(
                    "Invalid HTTP method: " + requestDto.getMethodRaw(),
                    requestDto.getMethodRaw(),
                    requestDto.getUri(),
                    ex
            );
        }

        // parameters
        if (foundByte == QUESTION) {
            foundByte = parseParameters(requestDto, inputByteBuffer, stringBuilder);
            if (END_LINE_WITH_ZERO.contains(foundByte)) {
                throw ErrorFactory.badRequest("HTTP version not specified");
            }
        }
        // http version
        parsePart(inputByteBuffer, END_LINE, stringBuilder);
        if (stringBuilder.isEmpty()) {
            throw ErrorFactory.badRequest("HTTP request cannot be empty");
        }
        String httpVersion = stringBuilder.toString();
        if (!httpVersion.equals("HTTP/1.1")) {
            throw ErrorFactory.badRequest("Unsupported HTTP protocol version");
        }
        requestDto.setHttpVersion(httpVersion);

    }

    private static byte parsePart(ByteBuffer inputByteBuffer, Set<Byte> stopChars, StringBuilder stringBuilder) throws AppException {
        return parsePart(inputByteBuffer, stopChars, stringBuilder, null, null);
    }

    private static byte parsePart(ByteBuffer inputByteBuffer, Set<Byte> stopChars, StringBuilder stringBuilder, Set<Byte> exceptionChar, String desc) throws AppException {
        stringBuilder.setLength(0);
        int countQuotes = 0;
        while (inputByteBuffer.hasRemaining()) {
            byte code_character = inputByteBuffer.get();

            if (exceptionChar != null ) {
                if (code_character == QUOTES) countQuotes++;
                if (exceptionChar.contains(code_character) && (countQuotes == 0 || countQuotes % 2 != 0)) throw ErrorFactory.badRequest(desc);
            }

            if (stopChars.contains(code_character)) {
                switch (code_character) {
                    case CR:
                        if (inputByteBuffer.hasRemaining() && inputByteBuffer.get() != LF)
                            throw ErrorFactory.badRequest("Carriage return character not at end of line");
                        break;
                    case LF:
                        throw ErrorFactory.badRequest("Line break character without preceding carriage break character");
                    default:
                        return code_character;
                }
                return code_character;
            }
            stringBuilder.append((char) code_character);
        }
        return 0;
    }


    private static void parseHeaders(RequestDto requestDto, ByteBuffer inputByteBuffer, StringBuilder stringBuilder) throws AppException {
        while (true) {
            String headerName = parseHeaderName(inputByteBuffer, stringBuilder);
            if (headerName.isEmpty()) break;

            String headerValue = parseHeaderValue(inputByteBuffer, stringBuilder);
            requestDto.addHeader(headerName, headerValue);
        }
    }

    private static String parseHeaderName(ByteBuffer inputByteBuffer, StringBuilder stringBuilder) throws AppException {
        stringBuilder.setLength(0);
        while (inputByteBuffer.hasRemaining()) {
            byte readByte = inputByteBuffer.get();
            if (readByte == COLON) {
                skip(inputByteBuffer, 1);
                break;
            }
            if (readByte == CR) {
                skipUntil(inputByteBuffer, LF);
                return "";
            }
            stringBuilder.append((char) readByte);
        }
        return stringBuilder.toString().trim();
    }

    private static String parseHeaderValue(ByteBuffer inputByteBuffer, StringBuilder stringBuilder) throws AppException {
        stringBuilder.setLength(0);
        while (inputByteBuffer.hasRemaining()) {
            byte readByte = inputByteBuffer.get();
            if (readByte == CR) {
                skipUntil(inputByteBuffer, LF);
                break;
            }
            stringBuilder.append((char) readByte);
        }
        return stringBuilder.toString().trim();
    }
}
