package http.server.processors;

import http.server.Context;
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

public class CreateVisitProcessor implements RequestProcessor {
    private static final Logger logger = LogManager.getLogger(CreateVisitProcessor.class);
    private final Repository repository;

    public CreateVisitProcessor(Repository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(Context context, SocketChannel clientChannel, ByteBuffer inputByteBuffer) throws AppException {
        logger.trace("CreateVisitProcessor start");
        RequestDto requestDto = context.getParsingResult().getValue().get();
//        int bytesReceived = context.getLengthInputBuffer();
//        int bytesParsed = requestDto.getBytesParsed();

        String contentLengthString = requestDto.getValueFromHeader("Content-Length");
        int contentLength = (contentLengthString == null) ? -1 : Integer.parseInt(contentLengthString);
        byte[] bytesBody = new byte[contentLength];
        inputByteBuffer.get(bytesBody, 0, inputByteBuffer.remaining());
        String body = new String(bytesBody, StandardCharsets.UTF_8);
        logger.trace("body = {}", body);
        Visit newVisit = Repository.getGson().fromJson(body, Visit.class);
        logger.debug("newVisit = {}", newVisit);
        if (!repository.saveVisit(newVisit)) {
            throw ErrorFactory.internalServerError("Failed to write Visit");
        }
    }
}
