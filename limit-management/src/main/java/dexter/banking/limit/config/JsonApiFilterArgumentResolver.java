package dexter.banking.limit.config;

import cz.jirutka.rsql.parser.ast.Node;
import dexter.banking.limit.web.JsonApiFilter;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.HashMap;
import java.util.Map;

@Component
public class JsonApiFilterArgumentResolver implements HandlerMethodArgumentResolver {

    private final JsonApiRsqlParser rsqlParser;

    public JsonApiFilterArgumentResolver(JsonApiRsqlParser rsqlParser) {
        this.rsqlParser = rsqlParser;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(JsonApiFilter.class)
                && parameter.getParameterType().equals(Node.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mav,
                                  NativeWebRequest webRequest, WebDataBinderFactory binder) {

        Map<String, String> params = new HashMap<>();
        webRequest.getParameterNames().forEachRemaining(name ->
                params.put(name, webRequest.getParameter(name)));

        return rsqlParser.parse(params);
    }
}