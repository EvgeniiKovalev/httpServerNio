package http.server;

import http.server.error.ErrorDto;
import http.server.parser.ParsingResult;
import io.vavr.control.Either;

import java.nio.ByteBuffer;

public class Context implements AutoCloseable {
    private ParsingResult parsingResult;
    private RequestAnswer requestAnswer;
    private boolean isClosed;
    private ByteBuffer inputBuffer;

    private int lengthInputBuffer;

    public Context() {
    }

    public ByteBuffer getInputBuffer() {
        return inputBuffer;
    }

    public void setInputBuffer(ByteBuffer inputBuffer) {
        this.inputBuffer = inputBuffer;
    }

    public int getLengthInputBuffer() {
        return lengthInputBuffer;
    }

    public void setLengthInputBuffer(int lengthInputBuffer) {
        this.lengthInputBuffer = lengthInputBuffer;
    }

    public void setErrorParsingResult(ErrorDto errorDto) {
        parsingResult.setValue(Either.left(errorDto));
    }

    public ParsingResult getParsingResult() {
        return parsingResult;
    }

    public void setParsingResult(ParsingResult parsingResult) {
        this.parsingResult = parsingResult;
    }

    public RequestAnswer getRequestAnswer() {
        return requestAnswer;
    }

    public void setRequestAnswer(RequestAnswer requestAnswer) {
        this.requestAnswer = requestAnswer;
    }

    public String getRoutingKey() {
        return parsingResult.getRoutingKey();
    }

    @Override
    public void close() throws Exception {
        if (isClosed) return;
        try {
            try {
                if (parsingResult != null) {
                    parsingResult.close();
                }
            } finally {
                if (requestAnswer != null) {
                    requestAnswer.close();
                }
            }
        } finally {
            parsingResult = null;
            requestAnswer = null;
            inputBuffer = null;
            isClosed = true;
        }
    }
}