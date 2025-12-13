package dexter.banking.limit.config;

import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.data.web.HateoasSortHandlerMethodArgumentResolver;
import org.springframework.lang.Nullable;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * A HATEOAS-aware Pageable resolver that formats URLs according to JSON:API specification.
 * This class is used by PagedResourcesAssembler to build correct "next", "prev", "first", "last" links.
 */
public class JsonApiHateoasPageableHandlerMethodArgumentResolver extends HateoasPageableHandlerMethodArgumentResolver {

    public JsonApiHateoasPageableHandlerMethodArgumentResolver(HateoasSortHandlerMethodArgumentResolver sortResolver) {
        super(sortResolver);
        setPageParameterName(JsonApiConstants.PAGE_NUMBER);
        setSizeParameterName(JsonApiConstants.PAGE_SIZE);
        setOneIndexedParameters(true);
    }

    @Override
    public void enhance(UriComponentsBuilder builder, @Nullable MethodParameter parameter, Object value) {
        // 1. Clean up existing pagination params (both encoded and unencoded variants)
        builder.replaceQueryParam(JsonApiConstants.PAGE_NUMBER);
        builder.replaceQueryParam(JsonApiConstants.PAGE_SIZE);
        builder.replaceQueryParam("page%5Bnumber%5D"); // The encoded key
        builder.replaceQueryParam("page%5Bsize%5D");   // The encoded key

        // 2. Let the parent add the new ones (it uses the unencoded names we set in constructor)
        super.enhance(builder, parameter, value);
    }
}