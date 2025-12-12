package dexter.banking.limit.config;

import dexter.banking.limit.web.JsonApiController;
import org.springframework.core.MethodParameter;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;

public class JsonApiPageableResolver extends PageableHandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return super.supportsParameter(parameter) &&
                parameter.getDeclaringClass().isAnnotationPresent(JsonApiController.class);
    }
}