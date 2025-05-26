package http.server.processors;

import http.server.Context;
import http.server.error.ErrorFactory;
import http.server.parser.RequestDto;
import http.server.answer.RequestAnswer;
import http.server.application.Repository;
import http.server.error.ErrorDto;
import io.vavr.control.Either;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class GetVisitsProcessor implements RequestProcessor {
    private static final Logger logger = LogManager.getLogger(GetVisitsProcessor.class);
    private final Repository repository;

    public GetVisitsProcessor(Repository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(Context context, SocketChannel clientChannel) throws IOException {
        if (context == null || clientChannel == null) throw new IOException("Context or clientChannel is null");


        Either<ErrorDto, RequestDto> either = context.getParsingResult().getValue();
        ErrorDto errorDto = either.fold(
                error -> error,
                right -> {
                    return ErrorFactory.internalErrorDto("Unexpected Right value");
                });

        Either<ErrorDto, RequestDto> either = context.getParsingResult().getValue();
        try (RequestDto requestDto = either.fold(
                error -> {
//                    throw new IllegalStateException("Unexpected Left value");
                    ErrorFactory.internalErrorDto("Unexpected Left value");
                },
                request -> {
                    return request;
                })) {

            if (requestDto.getParameter("id") != null) {
                Long id = Long.parseLong(requestDto.getParameter("id"));

            }

            String responce = "";
            RequestAnswer requestAnswer = new RequestAnswer();
            requestAnswer.setByteBuffer(ByteBuffer.wrap(responce.getBytes(StandardCharsets.UTF_8)));
            context.setRequestAnswer(requestAnswer);
//        clientChannel.write(ByteBuffer.wrap(responce.getBytes(StandardCharsets.UTF_8)));
        }
    }
}
