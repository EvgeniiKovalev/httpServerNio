package http.server.processors;

import com.google.gson.Gson;
import http.server.Context;
import http.server.answer.RequestAnswer;
import http.server.application.Repository;
import http.server.application.Visit;
import http.server.error.ErrorFactory;
import http.server.parser.RequestDto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

public class GetVisitsProcessor implements RequestProcessor {
    private static final Logger logger = LogManager.getLogger(GetVisitsProcessor.class);
    private final Repository repository;

    public GetVisitsProcessor(Repository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public void execute(Context context, SocketChannel clientChannel, ByteBuffer inputByteBuffer) throws Exception {
        RequestDto requestDto = context.getParsingResult().getValue().get();
        Gson gson = new Gson();
        StringBuilder responce = new StringBuilder("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\n\r\n\\");
        if (requestDto.getParameter("id") != null) {
            int id = Integer.parseInt(requestDto.getParameter("id"));
            Visit visit = repository.getVisitById(id);
            if (visit == null) throw ErrorFactory.notFoundError("Visit not found with id: " + id);
            responce.append(gson.toJson(visit));
        } else {
            List<Visit> visits = repository.getAllVisits();
            responce.append(gson.toJson(visits));
        }
        RequestAnswer requestAnswer = new RequestAnswer();
        requestAnswer.setByteBuffer(ByteBuffer.wrap(responce.toString().getBytes(StandardCharsets.UTF_8)));
        context.setRequestAnswer(requestAnswer);
//        clientChannel.write(ByteBuffer.wrap(responce.getBytes(StandardCharsets.UTF_8)));
    }
}

