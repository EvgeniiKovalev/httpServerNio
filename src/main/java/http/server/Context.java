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
    private int lengthRequest = 0;

    public Context() {
    }

    public ByteBuffer getInputBuffer() {
        return inputBuffer;
    }

    public int getLengthRequest() {
        return lengthRequest;
    }

    public void setLengthRequest(int lengthRequest) {
        this.lengthRequest = lengthRequest;
    }

    public void incLengthRequest(int lengthRequest) {
        this.lengthRequest += lengthRequest;
    }

    public void setInputBuffer(ByteBuffer inputBuffer) {
        this.inputBuffer = inputBuffer;
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

    public String getUri() {
        return parsingResult.getUri();
    }

    public String getMethod() {
        return parsingResult.getMethod();
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