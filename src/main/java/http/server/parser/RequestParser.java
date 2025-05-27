package http.server.parser;

import http.server.error.AppException;
import http.server.HttpMethod;
import http.server.error.HttpErrorType;

import java.nio.ByteBuffer;
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
    private static final Set<Byte> SET_FOR_URI = Set.of(QUESTION, SPACE, CR, LF);
    private static final Set<Byte> END_LINE_WITH_SPACE = Set.of(SPACE, CR, LF);
    private static final Set<Byte> END_LINE_WITH_ZERO = Set.of(CR, LF, (byte) 0);
    private static final Set<Byte> END_LINE = Set.of(CR, LF);
    private static final Set<Byte> END_PARAM_NAME = Set.of(EQUALITY, AMP, SPACE, CR, LF);
    private static final Set<Byte> END_PARAM_VALUE = Set.of(AMP, SPACE, CR, LF);

    private static final ThreadLocal<StringBuilder> threadLocalStringBuilder = ThreadLocal.withInitial(() -> new StringBuilder(64));

    public static byte parseParameters(RequestDto requestDto, ByteBuffer inputByteBuffer, StringBuilder stringBuilder) {
        byte foundByte = 0;

        while (inputByteBuffer.hasRemaining()) {
            foundByte = parsePart(inputByteBuffer, END_PARAM_NAME, stringBuilder);
            if (foundByte != EQUALITY) {
                throw new AppException("Parameter name not specified in request URI", HttpErrorType.BAD_REQUEST);
            }
            if (stringBuilder.isEmpty()) {
                throw new AppException("Parameter name cannot be empty in request URI", HttpErrorType.BAD_REQUEST);
            }
            String name = stringBuilder.toString();
            foundByte = parsePart(inputByteBuffer, END_PARAM_VALUE, stringBuilder);
            if (stringBuilder.isEmpty()) {
                throw new AppException("Parameter value with parameter name '" + name + "' cannot be empty in request URI", HttpErrorType.BAD_REQUEST);
            }
            requestDto.addParameter(name, stringBuilder.toString());
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
            return ParsingResult.error(e.getErrorType(), e.getMessage());
        }
    }

    public static RequestDto parse(ByteBuffer inputByteBuffer) throws AppException {
        if (inputByteBuffer == null) {
            throw new AppException("inputByteBuffer is null", HttpErrorType.INTERNAL_SERVER_ERROR);
        }
        inputByteBuffer.flip();
        RequestDto requestDto = new RequestDto();
        StringBuilder stringBuilder = threadLocalStringBuilder.get();
        parseFirstLine(inputByteBuffer, stringBuilder, requestDto);
        parseHeaders(requestDto, inputByteBuffer, stringBuilder);
        requestDto.setBytesParsed(inputByteBuffer.position());
        inputByteBuffer.mark();
        return requestDto;
    }

    private static void skip(ByteBuffer inputByteBuffer, int n) {
        inputByteBuffer.position(inputByteBuffer.position() + n);
    }

    private static void skipUntil(ByteBuffer inputByteBuffer, byte stopByte) throws AppException {
        while (inputByteBuffer.hasRemaining()) {
            if (inputByteBuffer.get() == stopByte) return;
        }
        throw new AppException("Expected character with code '" + stopByte + "' not found", HttpErrorType.BAD_REQUEST);
    }

    private static void parseFirstLine(ByteBuffer inputByteBuffer, StringBuilder stringBuilder, RequestDto requestDto) throws AppException {
        // http method
        byte foundByte = parsePart(inputByteBuffer, END_LINE_WITH_SPACE, stringBuilder);
        try {
            requestDto.setMethod(HttpMethod.valueOf(stringBuilder.toString()));
        } catch (IllegalArgumentException e) {
            throw new AppException("Invalid HTTP method: " + stringBuilder, HttpErrorType.BAD_REQUEST, e);
        }
        if (END_LINE_WITH_ZERO.contains(foundByte)) {
            throw new AppException("HTTP request is invalid", HttpErrorType.BAD_REQUEST);
        }
        // uri
        foundByte = parsePart(inputByteBuffer, SET_FOR_URI, stringBuilder);
        if (END_LINE_WITH_ZERO.contains(foundByte)) {
            throw new AppException("HTTP version not specified", HttpErrorType.BAD_REQUEST);
        }
        boolean uriCanBeEmpty = (foundByte == QUESTION);
        if (!uriCanBeEmpty && stringBuilder.isEmpty()) {
            throw new AppException("URI not specified", HttpErrorType.BAD_REQUEST);
        }
        requestDto.setUri(stringBuilder.toString());
        // parameters
        if (foundByte == QUESTION) {
            foundByte = parseParameters(requestDto, inputByteBuffer, stringBuilder);
            if (END_LINE_WITH_ZERO.contains(foundByte)) {
                throw new AppException("HTTP version not specified", HttpErrorType.BAD_REQUEST);
            }
        }
        // http version
        foundByte = parsePart(inputByteBuffer, END_LINE, stringBuilder);
        System.out.println("foundByte = " + foundByte);
        if (stringBuilder.isEmpty()) {
            throw new AppException("HTTP request cannot be empty", HttpErrorType.BAD_REQUEST);
        }
        String httpVersion = stringBuilder.toString();
        if (!httpVersion.equals("HTTP/1.1")) {
            throw new AppException("Unsupported HTTP protocol version", HttpErrorType.BAD_REQUEST);
        }
        requestDto.setHttpVersion(httpVersion);

    }

    private static byte parsePart(ByteBuffer inputByteBuffer, Set<Byte> stopChars, StringBuilder stringBuilder) throws AppException {
        stringBuilder.setLength(0);
        while (inputByteBuffer.hasRemaining()) {
            byte code_character = inputByteBuffer.get();
            if (stopChars.contains(code_character)) {
                switch (code_character) {
                    case CR:
                        if (inputByteBuffer.hasRemaining() && inputByteBuffer.get() != LF)
                            throw new AppException("Carriage return character not at end of line", HttpErrorType.BAD_REQUEST);
                        break;
                    case LF:
                        throw new AppException("Line break character without preceding carriage break character", HttpErrorType.BAD_REQUEST);
                    default:
                        return code_character;
                }
                return code_character;
            }
            stringBuilder.append((char) code_character);
        }
        return 0;
    }

    //todo проверить для POST что не читает лишнего в header
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
