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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class CreateVisitProcessor implements RequestProcessor {
    private static final Logger logger = LogManager.getLogger(CreateVisitProcessor.class);
    private static final Charset utf8 = StandardCharsets.UTF_8;
    private static final DateTimeFormatter formatterTs = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final Repository repository;

    public CreateVisitProcessor(Repository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public void execute(Context context, SocketChannel clientChannel, ByteBuffer inputByteBuffer) throws Exception {
        logger.trace("CreateVisitProcessor start");
        RequestDto requestDto = context.getParsingResult().getValue().get();
//        logger.trace("\"requestDto\"= " + requestDto);
        String contentLengthString = requestDto.getValueFromHeader("Content-Length");
        int contentLength = (contentLengthString == null) ? -1 : Integer.parseInt(contentLengthString);
        byte[] bytesBody = new byte[contentLength];
        inputByteBuffer.get(bytesBody, 0, inputByteBuffer.remaining());
        String body = new String(bytesBody, utf8);
        logger.trace("body = {}", body);
        Visit visit = Repository.getGson().fromJson(body, Visit.class);
        logger.trace("visit = {}", visit);
        try {
            visit.validatePeriod(repository);
            logger.trace("visit success validate");
        } catch (AppException e) {
            logger.trace("visit error validate:  {}", e.getMessage());
            throw e;
        }
        if (!repository.insertVisit(visit)) {
            throw ErrorFactory.internalServerError("Failed to add new Visit");
        }
        RequestAnswer.answer(context, "HTTP/1.1 201 Created\r\nContent-Type: text/html\r\n\r\n");
    }
}
