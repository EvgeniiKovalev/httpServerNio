package http.server.processors;

import http.server.Context;
import http.server.parser.RequestDto;
import http.server.answer.RequestAnswer;
import http.server.error.ErrorDto;
import http.server.error.HttpErrorType;
import io.vavr.control.Either;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class ErrorProcessor implements RequestProcessor {
    private static final Logger logger = LogManager.getLogger(ErrorProcessor.class);

    @Override
    public void execute(Context context, SocketChannel clientChannel) throws IOException {
        if (context == null) throw new IOException("Context is null");

        Either<ErrorDto, RequestDto> either = context.getParsingResult().getValue();
        try (ErrorDto errorDto = either.fold(error -> error, request -> {
            throw new IllegalStateException("Unexpected Right value");
        })) {
            HttpErrorType httpErrorType = errorDto.getErrorType();
            String responce = String.format("HTTP/1.1 %d %s\r\n" +
                            "Content-Type: text/html\r\n" +
                            "<html><body><h1>%s</h1></body></html>",
                    httpErrorType.getStatusCode(),
                    errorDto.getDescription(),
                    httpErrorType.getErrorCode());
            RequestAnswer requestAnswer = new RequestAnswer();
            requestAnswer.setByteBuffer(ByteBuffer.wrap(responce.getBytes(StandardCharsets.UTF_8)));
            context.setRequestAnswer(requestAnswer);
//        clientChannel.write(ByteBuffer.wrap(responce.getBytes(StandardCharsets.UTF_8)));
        }
    }
}
