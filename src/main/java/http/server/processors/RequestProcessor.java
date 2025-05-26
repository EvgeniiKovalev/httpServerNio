package http.server.processors;

import http.server.Context;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public interface RequestProcessor {
    void execute(Context context,
                 SocketChannel clientChannel/* если все таки не нужен -> убрать*/) throws IOException;
}
