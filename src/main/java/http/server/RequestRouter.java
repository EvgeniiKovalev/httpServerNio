package http.server;

import http.server.application.Repository;
import http.server.error.AppException;
import http.server.error.ErrorDto;
import http.server.error.ErrorFactory;
import http.server.error.HttpErrorType;
import http.server.processors.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RequestRouter {
    private static final Logger logger = LogManager.getLogger(RequestRouter.class);
    private static Map<String, RequestProcessor> processors;
    private static RequestProcessor errorProcessor;
    private static Set<String> methodHttp;
    private static Set<String> paths;


    public RequestRouter(Repository repository) {
        processors = Map.copyOf(initProcessors(repository));
        methodHttp = Set.copyOf(splitProcessors(processors, 0));
        paths = Set.copyOf(splitProcessors(processors, 1));
        errorProcessor = processors.get(ErrorProcessor.class.getSimpleName());
        if (errorProcessor == null) {
            throw new RuntimeException("ErrorProcessor must be registered in processors");
        }
    }

    private static RequestProcessor getErrorProcessor() {
        return errorProcessor;
    }

    private static RequestProcessor getProcessor(String routingKey) {
        return processors.get(routingKey);
    }

    private Map<String, RequestProcessor> initProcessors(Repository repository) {
        Map<String, RequestProcessor> result = new HashMap<>(6);
        result.put(ErrorProcessor.class.getSimpleName(), new ErrorProcessor());
        result.put("GET /visits", new GetVisitsProcessor(repository));
        result.put("POST /visits", new CreateVisitProcessor(repository));
        result.put("DELETE /visits", new DeleteVisitProcessor(repository));
        result.put("PUT /visits", new PutVisitProcessor(repository));

        return result;
    }

    private Set<String> splitProcessors(Map<String, RequestProcessor> processors, int index) {
        Set<String> result = new HashSet<>();
        for (String key : processors.keySet()) {
            String[] keySplitted = key.split(" ", 2);
            if (keySplitted.length > index)
                result.add(keySplitted[index]);
        }
        return result;
    }

    public void route(Context context, SocketChannel clientChannel, ByteBuffer inputByteBuffer) throws Exception {
        String routingKey = context.getRoutingKey();
        RequestProcessor requestProcessor = getProcessor(routingKey);

        if ((requestProcessor == null || requestProcessor == getErrorProcessor()) &&
                !methodHttp.contains(context.getMethod()) && paths.contains(context.getUri())) {
            ErrorDto errorDto = ErrorFactory.createErrorDto(HttpErrorType.METHOD_NOT_ALLOWED,
                    "METHOD NOT ALLOWED. Please use a different HTTP method for this paths");
            context.setErrorParsingResult(errorDto);
            requestProcessor = getErrorProcessor();

        } else if (requestProcessor == null) {
            requestProcessor = getErrorProcessor();
            logger.warn("Processor not found by routingKey '{}', process processed by processor '{}'",
                    routingKey, requestProcessor.getClass().getSimpleName());
            ErrorDto errorDto = ErrorFactory.notFoundErrorDto("RESOURCE NOT FOUND");
            context.setErrorParsingResult(errorDto);
        }

        try {
            requestProcessor.execute(context, clientChannel, inputByteBuffer);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            RequestProcessor errorProcessor = getErrorProcessor();
            ErrorDto errorDto;
            if (e instanceof AppException appException) {
                errorDto = ErrorFactory.createErrorDto(appException);
            } else {
                errorDto = ErrorFactory.createErrorDto(HttpErrorType.INTERNAL_SERVER_ERROR, e.getMessage());
            }
            context.setErrorParsingResult(errorDto);
            errorProcessor.execute(context, clientChannel, inputByteBuffer);
        }
    }
}
