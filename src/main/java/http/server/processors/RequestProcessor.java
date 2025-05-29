package http.server.processors;

import http.server.Context;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public interface RequestProcessor {
    void execute(Context context,
                 SocketChannel clientChannel,
                 ByteBuffer inputByteBuffer) throws Exception;

}
