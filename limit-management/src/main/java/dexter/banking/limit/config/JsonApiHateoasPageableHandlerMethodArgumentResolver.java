package dexter.banking.limit.config;

import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.data.web.HateoasSortHandlerMethodArgumentResolver;

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
}