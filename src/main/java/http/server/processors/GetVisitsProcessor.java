package http.server.processors;

import com.google.gson.Gson;
import http.server.Context;
import http.server.RequestAnswer;
import http.server.application.Repository;
import http.server.application.Visit;
import http.server.error.AppException;
import http.server.error.ErrorFactory;
import http.server.parser.RequestDto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public class GetVisitsProcessor implements RequestProcessor {
    private static final Logger logger = LogManager.getLogger(GetVisitsProcessor.class);
    private final Repository repository;

    public GetVisitsProcessor(Repository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public void execute(Context context, SocketChannel clientChannel, ByteBuffer inputByteBuffer) throws AppException {
        logger.trace("GetVisitsProcessor start");
        RequestDto requestDto = context.getParsingResult().getValue().get();
        StringBuilder responce = new StringBuilder("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\n\r\n\\");
        Gson gson = Repository.getGson();
        if (requestDto.getParameter("id") != null) {
            int id = Integer.parseInt(requestDto.getParameter("id"));
            Visit visit;
            try {
                visit = repository.getVisitById(id);
            } catch (SQLException e) {
                logger.error(e.getMessage(), e);
                throw ErrorFactory.internalServerError("Internal server error searching for Visit with id = " + id);
            }
            if (visit == null) throw ErrorFactory.notFoundError("Visit not found with id: " + id);
            String json = gson.toJson(visit);
            logger.trace(json);
            responce.append(json);
        } else {
            List<Visit> visits;
            try {
                visits = repository.getAllVisits();
            } catch (SQLException e) {
                logger.error(e.getMessage(), e);
                throw ErrorFactory.internalServerError("Internal server error get all Visits");
            }
            String json = gson.toJson(visits);
            logger.trace(json);
            responce.append(gson.toJson(visits));
        }
        RequestAnswer requestAnswer = new RequestAnswer();
        requestAnswer.setByteBuffer(ByteBuffer.wrap(responce.toString().getBytes(StandardCharsets.UTF_8)));
        context.setRequestAnswer(requestAnswer);
    }
}

