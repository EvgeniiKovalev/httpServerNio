package http.server;

import http.server.answer.RequestAnswer;
import http.server.error.ErrorDto;
import http.server.error.HttpErrorType;
import http.server.parser.ParsingResult;
import io.vavr.control.Either;

public class Context implements AutoCloseable {
    private ParsingResult parsingResult;
    private RequestAnswer requestAnswer;
    private boolean isClosed;

    public Context() {
    }

//    public Context ErrorContext(HttpErrorType httpErrorType, String errorMessage) {
//        Context result = new Context();
//        result.parsingResult = ParsingResult.error(httpErrorType, errorMessage);
//        return result;
//    }

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