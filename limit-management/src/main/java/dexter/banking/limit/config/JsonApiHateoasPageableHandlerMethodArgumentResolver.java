package dexter.banking.limit.config;

import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

public class JsonApiHateoasPageableHandlerMethodArgumentResolver extends HateoasPageableHandlerMethodArgumentResolver {

    public JsonApiHateoasPageableHandlerMethodArgumentResolver() {
        // Configure the resolver to recognize JSON:API style pagination parameters
        // This helps in correctly identifying the current page from the request.
        setPageParameterName("page[number]");
        setSizeParameterName("page[size]");
        setOneIndexedParameters(true);
    }

    /**
     * Overrides the default enhancement to build JSON:API compliant URLs.
     * The default implementation would create links like "?page=1&size=10&sort=name,asc",
     * which is not what we want.
     */
    @Override
    public void enhance(UriComponentsBuilder builder, MethodParameter parameter, Object value) {
        if (!(value instanceof Pageable pageable)) {
            return;
        }

        // 1. Handle Pagination (page[number], page[size])
        if (pageable.isPaged()) {
            builder.replaceQueryParam(getPageParameterName(), pageable.getPageNumber());
            builder.replaceQueryParam(getSizeParameterName(), pageable.getPageSize());
        }

        // 2. Add Sort Parameters
        Sort sort = pageable.getSort();
        if (sort.isSorted()) {
            List<String> sortParams = new ArrayList<>();
            for (Sort.Order order : sort) {
                String prefix = order.isDescending() ? "-" : "";
                sortParams.add(prefix + order.getProperty());
            }
            builder.replaceQueryParam("sort", String.join(",", sortParams));
        }
    }
}