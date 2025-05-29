package http.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class RequestAnswer implements AutoCloseable {
    boolean isClosed;
    private ByteBuffer byteBuffer;

    public RequestAnswer() {
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    public void setByteBuffer(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    public static void answer(Context context, String responce) {
        RequestAnswer requestAnswer = new RequestAnswer();
        requestAnswer.setByteBuffer(ByteBuffer.wrap(responce.getBytes(StandardCharsets.UTF_8)));
        context.setRequestAnswer(requestAnswer);
    }

    @Override
    public void close() throws IOException {
        if (isClosed) return;
        try {
            if (byteBuffer != null) {
                byteBuffer.clear();
                byteBuffer = null;
            }
        } finally {
            isClosed = true;
        }
    }
}
