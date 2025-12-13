package dexter.banking.limit.config;

import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.HateoasSortHandlerMethodArgumentResolver;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

public class JsonApiHateoasSortHandlerMethodArgumentResolver extends HateoasSortHandlerMethodArgumentResolver {

    @Override
    public void enhance(UriComponentsBuilder builder, MethodParameter parameter, Object value) {
        if (!(value instanceof Sort sort)) {
            return;
        }

        if (sort.isSorted()) {
            List<String> sortParams = new ArrayList<>();
            for (Sort.Order order : sort) {
                String prefix = order.isDescending() ? "-" : "";
                sortParams.add(prefix + order.getProperty());
            }
            builder.replaceQueryParam(JsonApiConstants.SORT, String.join(",", sortParams));
        } else {
            builder.replaceQueryParam(JsonApiConstants.SORT);
        }
    }
}