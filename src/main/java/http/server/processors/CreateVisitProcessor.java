package http.server.processors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import http.server.Context;
import http.server.application.LocalDateTimeAdapter;
import http.server.application.Repository;
import http.server.application.Visit;
import http.server.parser.RequestDto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

public class CreateVisitProcessor implements RequestProcessor {
    private static final Logger logger = LogManager.getLogger(CreateVisitProcessor.class);
    private final Repository repository;

    public CreateVisitProcessor(Repository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(Context context, SocketChannel clientChannel, ByteBuffer inputByteBuffer) throws Exception {
        logger.debug("CreateVisitProcessor start");
        RequestDto requestDto = context.getParsingResult().getValue().get();
//        int bytesReceived = context.getLengthInputBuffer();
//        int bytesParsed = requestDto.getBytesParsed();

        String contentLengthString = requestDto.getValueFromHeader("Content-Length");
        int contentLength = (contentLengthString == null) ? -1 : Integer.parseInt(contentLengthString);
        byte[] bytesBody = new byte[contentLength];
        inputByteBuffer.get(bytesBody, 0, inputByteBuffer.remaining());
        String body = new String(bytesBody, StandardCharsets.UTF_8);
        logger.trace("body = {}", body);
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .excludeFieldsWithoutExposeAnnotation()
                .create();

        Visit newVisit = gson.fromJson(body, Visit.class);
        //repository.saveVisit(newVisit);
        logger.debug("newVisit = {}", newVisit);



//        inputByteBuffer.get(bytesBody, 0, inputByteBuffer.remaining());
//        String body = URLDecoder.decode(new String(bytesBody, StandardCharsets.UTF_8), StandardCharsets.UTF_8);
//        logger.debug("body = \"{}\"", body);
//        Gson gson = new Gson();
//        Visit newVisit = gson.fromJson(body, Visit.class);
//        logger.debug("newVisit = {}", newVisit);

        //RequestDto requestDto = new RequestDto();
//        RequestParser.parseParameters(requestDto, inputByteBuffer, new StringBuilder());
//        Iterator<Map.Entry<String, String>> iterator = requestDto.getHeadersIterator();
//        while (iterator.hasNext()) {
//            Map.Entry<String, String> entry = iterator.next();
//            logger.debug("key : \"{}\", value : \"{}\"", entry.getKey(), entry.getValue());
//        }

//        logger.debug(body);

//        logger.debug("bytesReceived = {}, bytesParsed = {}, contentLength = {}, position = {}, limit = {} ",
//                bytesReceived, bytesParsed, contentLength, inputByteBuffer.position(), inputByteBuffer.limit());

//        logger.debug("bytesReceived = {}, bytesParsed = {}, contentLength = {}, position = {}, limit = {} ",
//                bytesReceived, bytesParsed, contentLength, inputByteBuffer.position(), inputByteBuffer.limit());


//        if (inputByteBuffer.position() < inputByteBuffer.limit()) {
//
//        }
//        int pos = inputByteBuffer.position();
//        int limit = inputByteBuffer.limit();
//        int capacity = inputByteBuffer.capacity();
//        StringBuilder action = new StringBuilder();
//
//        if (pos == limit) action.append("When parsing, fully read the inputByteBuffer.\r\n");
//        if (limit < capacity) action.append("The customer completely gave away all the data that was collected.\r\n");
//        if (limit == capacity)
//        if (pos < limit) {
//
//        }

//        Gson gson = new Gson();
//        Visit visit = gson.fromJson(context.re)
        //на данном этапе прочитано в только первые 8kb переданных данных,
        // из которых распарсена только заголовочная часть заголовочная часть данных от клиента

    }
}
