package http.server.processors;

import http.server.BuilderSimpleAnswer;
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
//        logger.trace("GetVisitsProcessor start");
        RequestDto requestDto = context.getParsingResult().getValue().get();
        StringBuilder responce = new StringBuilder(BuilderSimpleAnswer.header(200,null,"application/json"));

        if (requestDto.getParameter("id") == null
                && !requestDto.getParametersIterator().hasNext()) //protection against control characters in parameters?? and so on.
        {
            getAllVizit(responce);
        } else if (requestDto.getParameter("id") != null) {
            getOneVizit(requestDto, responce);
        } else {
            throw ErrorFactory.badRequest("INCORRECT_REQUEST_PARAMETER");
        }

        RequestAnswer.answer(context, String.valueOf(responce));
    }

    private void getAllVizit(StringBuilder responce) {
        List<Visit> visits;
        try {
            visits = repository.getAllVisits();
        } catch (SQLException e) {
            throw ErrorFactory.internalServerError("Internal server error get all Visits");
        }
        String json = Repository.getGson().toJson(visits);
//        logger.trace(json);
        responce.append(json);
    }

    private void getOneVizit(RequestDto requestDto, StringBuilder responce) {
        int id;
        try {
            id = Integer.parseInt(requestDto.getParameter("id"));
        } catch (NumberFormatException e) {
            throw ErrorFactory.badRequest(String.format("The value of the 'id' parameter is not a number:'%s'",
                    requestDto.getParameter("id")), "INCORRECT_REQUEST_PARAMETER");
        }

        Visit visit;
        try {
            visit = repository.getVisitById(id);
        } catch (SQLException e) {
            throw ErrorFactory.internalServerError("Internal server error searching for Visit with id = " + id);
        }
        if (visit == null) throw ErrorFactory.notFoundError("Visit not found with id: " + id);
        String json = Repository.getGson().toJson(visit);
//        logger.trace(json);
        responce.append(json);
    }
}

