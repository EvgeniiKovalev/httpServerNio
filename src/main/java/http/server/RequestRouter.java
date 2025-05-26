package http.server;

import http.server.application.Repository;
import http.server.error.ErrorDto;
import http.server.parser.ParsingResult;
import http.server.parser.RequestDto;
import http.server.processors.ErrorProcessor;
import http.server.processors.GetVisitsProcessor;
import http.server.processors.RequestProcessor;
import io.vavr.control.Either;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

public class RequestRouter implements RequestProcessor{
    private final Map<String, RequestProcessor> processors;
    private final Repository repository;

    public RequestRouter(Repository repository) {
        this.repository = repository;
        processors = Map.copyOf(initProcessors(repository));

    }

    private Map<String, RequestProcessor> initProcessors(Repository repository) {
        Map<String, RequestProcessor> result = new HashMap<>(6);
        result.put(ErrorProcessor.class.getSimpleName(), new ErrorProcessor());
        result.put("GET /visit", new GetVisitsProcessor(repository));

        return result;
    }

    private RequestProcessor getErrorProcessor() {
        return processors.get(ErrorProcessor.class.getSimpleName());
    }

    private RequestProcessor getProcessor(String routingKey) {
        RequestProcessor result = processors.get(routingKey);
        return result != null ? result : getErrorProcessor();
    }

    @Override
    public void execute(Context context, SocketChannel clientChannel) throws IOException {

        Either<ErrorDto, RequestDto> parsingResult = context.getParsingResult().getValue();
        RequestProcessor processor;
        if (parsingResult.isRight()) {
            processor = getProcessor(parsingResult.get().getRoutingKey());
        } else {
            processor = getErrorProcessor();
        }
        processor.execute(context, clientChannel);
    }
}
