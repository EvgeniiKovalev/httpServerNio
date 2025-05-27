package http.server;

import http.server.application.Repository;
import http.server.error.AppException;
import http.server.error.ErrorDto;
import http.server.error.ErrorFactory;
import http.server.error.HttpErrorType;
import http.server.processors.CreateVisitProcessor;
import http.server.processors.ErrorProcessor;
import http.server.processors.GetVisitsProcessor;
import http.server.processors.RequestProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

public class RequestRouter {
    private static final Logger logger = LogManager.getLogger(RequestRouter.class);
    private static Map<String, RequestProcessor> processors;

    public RequestRouter(Repository repository) {
        processors = Map.copyOf(initProcessors(repository));

        if (!processors.containsKey(ErrorProcessor.class.getSimpleName())) {
            throw new RuntimeException("ErrorProcessor must be registered in processors");
            //todo проверить что запишет в лог при отсутствии регистрации ErrorProcessor
        }
    }

    private static RequestProcessor getErrorProcessor() {
        return processors.get(ErrorProcessor.class.getSimpleName());
    }

    private static RequestProcessor getProcessor(String routingKey) {
        return processors.get(routingKey);
    }

    private Map<String, RequestProcessor> initProcessors(Repository repository) {
        Map<String, RequestProcessor> result = new HashMap<>(6);
        result.put(ErrorProcessor.class.getSimpleName(), new ErrorProcessor());
        result.put("GET /visits", new GetVisitsProcessor(repository));
        result.put("POST /visits", new CreateVisitProcessor(repository));

        return result;
    }

    /**
     * The first MAX_HTTP_HEADER_SIZE_KB of data from the client are read and parsed,
     * the rest of the client data is read and processed at the discretion of the processor
     *
     * @param context
     * @param clientChannel
     * @param inputByteBuffer
     * @throws Exception
     */
    public void route(Context context, SocketChannel clientChannel, ByteBuffer inputByteBuffer) throws Exception {
        String routingKey = context.getRoutingKey();
        RequestProcessor requestProcessor = getProcessor(routingKey);
        if (requestProcessor == null) {
            String message = "No processor found by key\"" + routingKey + "\" during routing";
            ErrorDto errorDto = ErrorFactory.notFoundErrorDto(message);
            context.setErrorParsingResult(errorDto);
            requestProcessor = getErrorProcessor();
            logger.error(message);
        }

        try {
            requestProcessor.execute(context, clientChannel, inputByteBuffer);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            RequestProcessor errorProcessor = getErrorProcessor();
            ErrorDto errorDto;
            HttpErrorType errorType;
            String message;
            if (e instanceof AppException appException) {
                errorType = appException.getErrorType();
                message = appException.getMessage();
            } else {
                errorType = HttpErrorType.INTERNAL_SERVER_ERROR;
                message = e.getMessage();
            }
            errorDto = ErrorFactory.createErrorDto(errorType, message);
            context.setErrorParsingResult(errorDto);
            errorProcessor.execute(context, clientChannel, inputByteBuffer);
        }
    }
}
