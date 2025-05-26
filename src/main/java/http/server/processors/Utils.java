package http.server.processors;

import java.util.function.Function;

public class Utils {

    public <T> T getAndValidateParam(
            String paramName,
            Function<String, T> funcParse,
            String errorFuncMessage
    ) {
        if (!containsParameter(paramName)) {
            throw new BadRequestException(
                    "INCORRECT_REQUEST_DATA",
                    "Missing \"" + paramName + "\" parameter"
            );
        }

        String paramValue = getParameter(paramName);
        try {
            return funcParse.apply(paramValue);
        } catch (Exception e) {
            throw new BadRequestException(
                    "INCORRECT_REQUEST_PARAMETER",
                    "Параметр '" + paramName + "' " + errorFuncMessage
            );
        }
    }

}
