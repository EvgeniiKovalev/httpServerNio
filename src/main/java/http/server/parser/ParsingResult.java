package http.server.parser;

import http.server.error.ErrorDto;
import http.server.error.ErrorFactory;
import http.server.error.HttpErrorType;
import io.vavr.control.Either;

import java.io.IOException;

public class ParsingResult implements AutoCloseable {
    private Either<ErrorDto, RequestDto> value;
    private boolean isClosed;



    private ParsingResult(Either<ErrorDto, RequestDto> value) {
        this.value = value;
    }

    public static ParsingResult success(RequestDto dto) {
        return new ParsingResult(Either.right(dto));
    }

    public static ParsingResult error(HttpErrorType errorType, String description) {
        return new ParsingResult(Either.left(
                ErrorFactory.createErrorDto(errorType, description)
        ));
    }

    public static ParsingResult badRequest(String description) {
        return error(HttpErrorType.BAD_REQUEST, description);
    }

    public static ParsingResult notFound(String description) {
        return error(HttpErrorType.NOT_FOUND, description);
    }

    public static ParsingResult internalError(String description) {
        return error(HttpErrorType.INTERNAL_SERVER_ERROR, description);
    }

    public Either<ErrorDto, RequestDto> getValue() {
        return value;
    }

    public void setValue(Either<ErrorDto, RequestDto> value) {
        this.value = value;
    }

    public boolean isSuccess() {
        return value.isRight();
    }

    public boolean isError() {
        return value.isLeft();
    }

    public int getBytesParsed() {
        return value.fold(
                error -> {return 0;},
                RequestDto::getBytesParsed
        );
    }

    public String getRoutingKey() {
        return value.fold(
                ErrorDto::getRoutingKey,
                RequestDto::getRoutingKey
        );
    }

    @Override
    public void close() throws IOException {
        if (isClosed) return;
        try {
            if (isSuccess()) {
                RequestDto requestDto = value.get();
                requestDto.close();
            } else {
                ErrorDto errorDto = value.getLeft();
                errorDto.close();
            }
        } catch (IOException e) {
            throw new IOException("Error closing ParsingResult {}", e);
        } finally {
            isClosed = true;
        }
    }
}
