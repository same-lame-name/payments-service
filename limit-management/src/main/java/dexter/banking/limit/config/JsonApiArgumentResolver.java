package dexter.banking.limit.config;

import cz.jirutka.rsql.parser.ast.Node;
import dexter.banking.limit.web.DomainQuery;
import dexter.banking.limit.web.JsonApiQuery;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JsonApiArgumentResolver implements HandlerMethodArgumentResolver {

    private final JsonApiRsqlParser rsqlParser;
    private final PageableHandlerMethodArgumentResolver pageResolver;

    public JsonApiArgumentResolver(JsonApiRsqlParser rsqlParser) {
        this.rsqlParser = rsqlParser;

        // We configure an internal delegate to handle the pagination/sorting complexity
        this.pageResolver = new PageableHandlerMethodArgumentResolver();
        this.pageResolver.setPageParameterName("page[number]");
        this.pageResolver.setSizeParameterName("page[size]");
        this.pageResolver.setOneIndexedParameters(true); // JSON:API standard
        this.pageResolver.setFallbackPageable(Pageable.ofSize(10));
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(JsonApiQuery.class)
                && parameter.getParameterType().equals(DomainQuery.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mav,
                                  NativeWebRequest webRequest, WebDataBinderFactory binder) {

        // 1. Delegate Pagination to Spring's logic
        Pageable pageable = pageResolver.resolveArgument(parameter, mav, webRequest, binder);

        // 2. Handle JSON:API Sorting (sort=-created,name)
        String sortParam = webRequest.getParameter("sort");
        if (sortParam != null && !sortParam.isBlank()) {
            Sort sort = parseSortParam(sortParam);
            if (pageable.isPaged()) {
                pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
            }
        }

        // 3. Delegate Filtering to our RSQL Parser
        Map<String, String> params = new HashMap<>();
        webRequest.getParameterNames().forEachRemaining(name ->
                params.put(name, webRequest.getParameter(name)));

        Node filterNode = rsqlParser.parse(params);

        return new DomainQuery(filterNode, pageable);
    }

    private Sort parseSortParam(String sortParam) {
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