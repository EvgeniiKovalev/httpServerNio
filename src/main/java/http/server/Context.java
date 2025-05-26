package http.server;

import http.server.answer.RequestAnswer;
import http.server.parser.ParsingResult;

public class Context implements AutoCloseable {
    private ParsingResult parsingResult;
    private RequestAnswer requestAnswer;
    private boolean isClosed;

    public Context() {
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

    public String getParsingResultRoutingKey() {
        return parsingResult.getRoutingKey();
    }

    public void setRequestAnswer(RequestAnswer requestAnswer) {
        this.requestAnswer = requestAnswer;
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
            isClosed = true;
        }
    }
}