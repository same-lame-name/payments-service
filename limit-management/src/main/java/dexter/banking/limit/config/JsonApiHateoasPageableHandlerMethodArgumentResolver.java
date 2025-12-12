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
        setPageParameterName("page[number]");
        setSizeParameterName("page[size]");
        setOneIndexedParameters(true);
    }

    @Override
    public void enhance(UriComponentsBuilder builder, MethodParameter parameter, Object value) {
        if (!(value instanceof Pageable pageable)) {
            return;
        }

        // 1. Handle Pagination (page[number], page[size])
        if (pageable.isPaged()) {
            int pageNumber = pageable.getPageNumber() + (isOneIndexedParameters() ? 1 : 0);
            builder.replaceQueryParam(getPageParameterName(), pageNumber);
            builder.replaceQueryParam(getSizeParameterName(), pageable.getPageSize());
            // Remove legacy Spring defaults if they differ
            builder.replaceQueryParam("page");
            builder.replaceQueryParam("size");
            // Remove encoded variants that might be treated as distinct keys
            builder.replaceQueryParam("page%5Bnumber%5D");
            builder.replaceQueryParam("page%5Bsize%5D");
        }

        // 2. Handle Sort (JSON:API style: -name,created)
        Sort sort = pageable.getSort();
        if (sort.isSorted()) {
            List<String> sortParams = new ArrayList<>();
            for (Sort.Order order : sort) {
                String prefix = order.isDescending() ? "-" : "";
                sortParams.add(prefix + order.getProperty());
            }
            builder.replaceQueryParam("sort", String.join(",", sortParams));
        } else {
            builder.replaceQueryParam("sort");
        }
    }
}