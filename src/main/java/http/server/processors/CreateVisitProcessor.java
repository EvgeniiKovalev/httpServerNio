package http.server.processors;

import com.google.gson.Gson;
import http.server.Context;
import http.server.application.Repository;
import http.server.application.Visit;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class CreateVisitProcessor implements RequestProcessor {
    private final Repository repository;

    public CreateVisitProcessor(Repository repository) {
        this.repository = repository;
    }


    @Override
    public void execute(Context context, SocketChannel clientChannel, ByteBuffer inputByteBuffer) throws Exception {
//        Gson gson = new Gson();
//        Visit visit = gson.fromJson(context.re)
        //на данном этапе прочитано в только первые 8kb переданных данных,
        // из которых распарсена только заголовочная часть заголовочная часть данных от клиента

    }
}
