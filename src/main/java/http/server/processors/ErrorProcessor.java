package http.server.processors;

import http.server.Context;
import http.server.answer.RequestAnswer;
import http.server.error.ErrorDto;
import http.server.error.ErrorFactory;
import http.server.error.HttpErrorType;
import http.server.parser.RequestDto;
import io.vavr.control.Either;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class ErrorProcessor implements RequestProcessor {
    private static final Logger logger = LogManager.getLogger(ErrorProcessor.class);

    @Override
    public void execute(Context context, SocketChannel clientChannel, ByteBuffer inputByteBuffer) throws Exception {
        if (context == null || clientChannel == null) throw new Exception("Context or clientChannel is null");
        Either<ErrorDto, RequestDto> either = context.getParsingResult().getValue();
        ErrorDto errorDto = either.fold(
                error -> error,
                right -> {
                    return ErrorFactory.internalErrorDto("Unexpected Right value");
                });

        HttpErrorType httpErrorType = errorDto.getErrorType();
        String errorDescription = errorDto.getDescription();
        String responce = String.format("HTTP/1.1 %d %s\r\n" +
                        "Content-Type: text/html\r\n" +
                        "\r\n<html><body><h1>%s</h1></body></html>",
                httpErrorType.getStatusCode(),
                httpErrorType.getErrorCode(),
                errorDescription);
        RequestAnswer requestAnswer = new RequestAnswer();
        requestAnswer.setByteBuffer(ByteBuffer.wrap(responce.getBytes(StandardCharsets.UTF_8)));
        context.setRequestAnswer(requestAnswer);
    }
}
