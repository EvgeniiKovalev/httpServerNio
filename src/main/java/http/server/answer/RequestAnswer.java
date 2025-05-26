package http.server.answer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class RequestAnswer implements AutoCloseable {
    private static final Logger logger = LogManager.getLogger(RequestAnswer.class);
    boolean isClosed;
    private ByteBuffer byteBuffer;   // Заголовки (готовы сразу)
    private FileChannel file;        // Файл для отправки (если > 4 МБ)
    private boolean isStreaming;     // Режим потоковой передачи
    private long bytesTransferred;   // Сколько уже отправили

    public RequestAnswer() {
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    public void setByteBuffer(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    public FileChannel getFile() {
        return file;
    }

    public void setFile(FileChannel file) {
        this.file = file;
    }

    public boolean isStreaming() {
        return isStreaming;
    }

    public void setStreaming(boolean streaming) {
        isStreaming = streaming;
    }

    public long getBytesTransferred() {
        return bytesTransferred;
    }

    public void setBytesTransferred(long bytesTransferred) {
        this.bytesTransferred = bytesTransferred;
    }

    @Override
    public void close() throws IOException {
        if (isClosed) return;
        try {
            if (file != null) {
                file.close();
                file = null;
            }
            if (byteBuffer != null) {
                byteBuffer.clear();
                byteBuffer = null;
            }
        } finally {
            isClosed = true;
        }
    }
}
