package http.server.processors;

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
import java.util.Objects;

public class PutVisitProcessor implements RequestProcessor {
    private static final Logger logger = LogManager.getLogger(PutVisitProcessor.class);
    private final Repository repository;

    public PutVisitProcessor(Repository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public void execute(Context context, SocketChannel clientChannel, ByteBuffer inputByteBuffer) throws Exception {
        logger.trace("PutVisitProcessor start");
        RequestDto requestDto = context.getParsingResult().getValue().get();
        String contentLengthString = requestDto.getValueFromHeader("Content-Length");
        int contentLength = (contentLengthString == null) ? -1 : Integer.parseInt(contentLengthString);
        byte[] bytesBody = new byte[contentLength];

        inputByteBuffer.get(bytesBody, 0, inputByteBuffer.remaining());
        String body = new String(bytesBody, StandardCharsets.UTF_8);
        logger.trace("body = {}", body);
        Visit visit = Repository.getGson().fromJson(body, Visit.class);

        if (visit.getId() <= 0)
            throw ErrorFactory.badRequest("Передан пустой id = '" + visit.getId() + "', for update");

        boolean existId;
        try {
            existId = repository.checkExistVisitById(visit.getId());
        } catch (Exception e) {
            logger.trace("visit error check exists id :  {}", e.getMessage());
            throw e;
        }
        if (!existId)
            throw ErrorFactory.notFoundError("No visit with id = '" + visit.getId() + "', for update");

        logger.trace("Found visit for update, with passed id = {}",  visit.getId());
        logger.trace("visit = {}", visit);

        try {
            visit.validatePeriod(repository);
            logger.trace("visit success validate");
        } catch (AppException e) {
            logger.trace("visit error validate:  {}", e.getMessage());
            throw e;
        }

        if (!repository.updateVisit(visit)) {
            throw ErrorFactory.internalServerError("Failed to update Visit");
        }
        RequestAnswer.answer(context, "HTTP/1.1 204 No Content\r\nContent-Type: application/json\r\n\r\n");
    }
}
