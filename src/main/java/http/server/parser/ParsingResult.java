package http.server.parser;

import http.server.error.AppException;
import http.server.error.ErrorDto;
import http.server.error.ErrorFactory;
import io.vavr.control.Either;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class ParsingResult implements AutoCloseable {
    private static final Logger logger = LogManager.getLogger(ParsingResult.class);
    private Either<ErrorDto, RequestDto> value;
    private boolean isClosed;


    private ParsingResult(Either<ErrorDto, RequestDto> value) {
        this.value = value;
    }

    public static ParsingResult success(RequestDto requestDto) {
        return new ParsingResult(Either.right(requestDto));
    }

    public static ParsingResult error(AppException ex) {
        logger.error(ex.getMessage(), ex);
        return new ParsingResult(Either.left(
                ErrorFactory.createErrorDto(ex)
        ));
    }

    public static ParsingResult error(ErrorDto errorDto) {
        return new ParsingResult(Either.left(errorDto));
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
                error -> 0,
                RequestDto::getBytesParsed
        );
    }

    public String getMethod() {
        return value.fold(
                ErrorDto::getMethodRaw ,
                RequestDto::getMethodRaw
        );
    }

    public String getUri() {
        return value.fold(
                ErrorDto::getUri,
                RequestDto::getUri
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
