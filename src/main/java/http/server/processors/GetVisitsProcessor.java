package http.server.processors;

import http.server.Context;
import http.server.RequestRouter;
import http.server.answer.RequestAnswer;
import http.server.application.Repository;
import http.server.application.Visit;
import http.server.error.HttpErrorType;
import http.server.parser.ParsingResult;
import http.server.parser.RequestDto;
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

        RequestDto requestDto = context.getParsingResult().getValue().get();
        if (requestDto.getParameter("id") != null) {
            int id = Integer.parseInt(requestDto.getParameter("id"));
            Visit visit = repository.getVisitById(id);
            if (visit == null) {
                context.setParsingResult(ParsingResult.error(HttpErrorType.NOT_FOUND, "Visit not found with id: " + id));
                RequestRouter.execute(context, clientChannel);
                return;
            }

        }

        String responce = "";
        RequestAnswer requestAnswer = new RequestAnswer();
        requestAnswer.setByteBuffer(ByteBuffer.wrap(responce.getBytes(StandardCharsets.UTF_8)));
        context.setRequestAnswer(requestAnswer);
//        clientChannel.write(ByteBuffer.wrap(responce.getBytes(StandardCharsets.UTF_8)));
    }
}

