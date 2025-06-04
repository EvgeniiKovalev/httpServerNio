package http.server.processors;

import http.server.BuilderSimpleAnswer;
import http.server.Context;
import http.server.RequestAnswer;
import http.server.application.Repository;
import http.server.error.AppException;
import http.server.error.ErrorFactory;
import http.server.parser.RequestDto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.sql.SQLException;
import java.util.Objects;

public class DeleteVisitProcessor implements RequestProcessor{
    private static final Logger logger = LogManager.getLogger(DeleteVisitProcessor.class);
    private final Repository repository;

    public DeleteVisitProcessor(Repository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public void execute(Context context, SocketChannel clientChannel, ByteBuffer inputByteBuffer) throws AppException {
        RequestDto requestDto = context.getParsingResult().getValue().get();
        if (requestDto.getParameter("id") == null) {
            throw ErrorFactory.badRequest("No deleted Visit id specified");
        }

        int statusCode = 204;
        int id = 0;
        try {
            id = Integer.parseInt(requestDto.getParameter("id"));
            if (repository.deleteVisitById(id)) {
                logger.info("Deleted Visit with id = {}", id);
            } else {
                logger.info("No deletion visit found with the specified id = {}", id);
                statusCode = 404;
            }
            RequestAnswer.answer(context, BuilderSimpleAnswer.header(statusCode, null, "text/html"));

        } catch (NumberFormatException e) {
            throw ErrorFactory.badRequest(
                    String.format("The value of the 'id' parameter is not a number:'%s'",
                            requestDto.getParameter("id")),
                    "INCORRECT_REQUEST_PARAMETER");
        } catch (SQLException e) {
            throw ErrorFactory.internalServerError("Internal server error deleting for Visit with id = " + id);
        }
    }
}
