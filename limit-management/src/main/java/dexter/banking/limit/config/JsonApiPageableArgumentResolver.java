package dexter.banking.limit.config;

import dexter.banking.limit.web.JsonApiPage;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class JsonApiPageableArgumentResolver extends PageableHandlerMethodArgumentResolver {

    public JsonApiPageableArgumentResolver() {
        this.setPageParameterName("page[number]");
        this.setSizeParameterName("page[size]");
        this.setOneIndexedParameters(true);
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(JsonApiPage.class)
                && parameter.getParameterType().equals(Pageable.class);
    }

    @Override
    public Pageable resolveArgument(MethodParameter methodParameter, ModelAndViewContainer mavContainer,
                                    NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        // Simply delegate to the parent class which is already configured for JSON:API
        return super.resolveArgument(methodParameter, mavContainer, webRequest, binderFactory);
    }
}