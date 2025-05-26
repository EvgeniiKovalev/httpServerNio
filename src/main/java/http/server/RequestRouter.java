package http.server;

import http.server.application.Repository;
import http.server.processors.ErrorProcessor;
import http.server.processors.GetVisitsProcessor;
import http.server.processors.RequestProcessor;

import java.util.HashMap;
import java.util.Map;

public class RequestRouter {
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

    public RequestProcessor getProcessor(String routingKey) {
        return processors.getOrDefault(routingKey, processors.get(ErrorProcessor.class.getSimpleName()));
    }
}
