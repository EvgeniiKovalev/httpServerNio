package http.server.processors;

import com.google.gson.Gson;
import http.server.Context;
import http.server.application.Repository;
import http.server.application.Visit;
import http.server.parser.RequestDto;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class CreateVisitProcessor implements RequestProcessor {
    private final Repository repository;

    public CreateVisitProcessor(Repository repository) {
        this.repository = repository;
    }


    @Override
    public void execute(Context context, SocketChannel clientChannel, ByteBuffer inputByteBuffer) throws Exception {
        RequestDto requestDto = context.getParsingResult().getValue().get();
//        if (requestDto.getBytesReceived() < )

        if (inputByteBuffer.position() < inputByteBuffer.limit()) {

        }
        int pos = inputByteBuffer.position();
        int limit = inputByteBuffer.limit();
        int capacity = inputByteBuffer.capacity();
        StringBuilder action = new StringBuilder();

        if (pos == limit) action.append("When parsing, fully read the inputByteBuffer.\r\n");
        if (limit < capacity) action.append("The customer completely gave away all the data that was collected.\r\n");
        if (limit == capacity)
        if (pos < limit) {

        }

//        Gson gson = new Gson();
//        Visit visit = gson.fromJson(context.re)
        //на данном этапе прочитано в только первые 8kb переданных данных,
        // из которых распарсена только заголовочная часть заголовочная часть данных от клиента

    }
}
