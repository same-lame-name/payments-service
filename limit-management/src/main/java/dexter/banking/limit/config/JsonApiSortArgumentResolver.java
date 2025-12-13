package dexter.banking.limit.config;

import dexter.banking.limit.web.JsonApiSort;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.ArrayList;
import java.util.List;

@Component
public class JsonApiSortArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(JsonApiSort.class)
                && parameter.getParameterType().equals(Sort.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mav,
                                  NativeWebRequest webRequest, WebDataBinderFactory binder) {

        String sortParam = webRequest.getParameter("sort");
        if (sortParam == null || sortParam.isBlank()) {
            return Sort.unsorted();
        }

        List<Sort.Order> orders = new ArrayList<>();
        String[] fields = sortParam.split(",");
        for (String field : fields) {
            if (field.startsWith("-")) {
                orders.add(Sort.Order.desc(field.substring(1)));
            } else {
                orders.add(Sort.Order.asc(field));
            }
        }
        return Sort.by(orders);
    }
}