package http.server.processors;

import http.server.BuilderSimpleAnswer;
import http.server.Context;
import http.server.RequestAnswer;
import http.server.error.AppException;
import http.server.error.ErrorDto;
import http.server.error.ErrorFactory;
import http.server.parser.RequestDto;
import io.vavr.control.Either;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ErrorProcessor implements RequestProcessor {
    private static final Logger logger = LogManager.getLogger(ErrorProcessor.class);

    @Override
    public void execute(Context context, SocketChannel clientChannel, ByteBuffer inputByteBuffer) throws AppException {
        logger.trace("ErrorProcessor start");
        Either<ErrorDto, RequestDto> either = context.getParsingResult().getValue();
        ErrorDto errorDto = either.fold(
                error -> error,
                right -> ErrorFactory.internalErrorDto("Unexpected Right value"));

        RequestAnswer.answer(context,
                BuilderSimpleAnswer.headerBody(
                        errorDto.getStatusCode(),
                        errorDto.getErrorCode(),
                        errorDto.getDescription(),
                        "text/html")
        );
//        RequestAnswer.answer(context,
//                String.format("HTTP/1.1 %d %s\r\nContent-Type: text/html\r\n\r\n<html><body><h1>%s</h1></body></html>",
//                        errorDto.getStatusCode(), errorDto.getErrorCode(), errorDto.getDescription())
//                );
    }
}
