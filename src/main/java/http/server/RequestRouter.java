package http.server;

import http.server.application.Repository;
import http.server.error.AppException;
import http.server.error.ErrorDto;
import http.server.error.ErrorFactory;
import http.server.error.HttpErrorType;
import http.server.parser.RequestDto;
import http.server.processors.CreateVisitProcessor;
import http.server.processors.ErrorProcessor;
import http.server.processors.GetVisitsProcessor;
import http.server.processors.RequestProcessor;
import io.vavr.control.Either;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

public class RequestRouter {
    private static final Logger logger = LogManager.getLogger(HttpServer.class);
    private static Map<String, RequestProcessor> processors;
    private final Repository repository;

    public RequestRouter(Repository repository) {
        this.repository = repository;
        processors = Map.copyOf(initProcessors(repository));

        if (!processors.containsKey(ErrorProcessor.class.getSimpleName())) {
            throw new RuntimeException("ErrorProcessor must be registered in processors");
            //todo проверить что запишет влог при отсутствии регистрации ErrorProcessor
        }
    }

    private static RequestProcessor getErrorProcessor() {
        return processors.get(ErrorProcessor.class.getSimpleName());
    }

    private static RequestProcessor getProcessor(String routingKey) {
        RequestProcessor result = processors.get(routingKey);
        return result != null ? result : getErrorProcessor();
    }

    private Map<String, RequestProcessor> initProcessors(Repository repository) {
        Map<String, RequestProcessor> result = new HashMap<>(6);
        result.put(ErrorProcessor.class.getSimpleName(), new ErrorProcessor());
        result.put("GET /visits", new GetVisitsProcessor(repository));
        result.put("POST /visits", new CreateVisitProcessor(repository));

        return result;
    }

    public void route(Context context, SocketChannel clientChannel, ByteBuffer inputByteBuffer) throws Exception {
        Either<ErrorDto, RequestDto> parsingResult = context.getParsingResult().getValue();
        RequestProcessor requestProcessor;

        if (parsingResult.isRight()) {
            requestProcessor = getProcessor(parsingResult.get().getRoutingKey());
        } else {
            requestProcessor = getErrorProcessor();
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
