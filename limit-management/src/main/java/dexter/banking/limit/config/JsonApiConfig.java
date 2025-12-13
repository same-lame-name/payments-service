package dexter.banking.limit.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toedter.spring.hateoas.jsonapi.JsonApiConfiguration;
import com.toedter.spring.hateoas.jsonapi.JsonApiMediaTypeConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class JsonApiConfig implements WebMvcConfigurer {

    private final JsonApiFilterArgumentResolver filterArgumentResolver;
    private final JsonApiSortArgumentResolver sortArgumentResolver;
    private final JsonApiPageableArgumentResolver pageableArgumentResolver;

    public JsonApiConfig(JsonApiFilterArgumentResolver filterArgumentResolver,
                         JsonApiSortArgumentResolver sortArgumentResolver,
                         JsonApiPageableArgumentResolver pageableArgumentResolver) {
        this.filterArgumentResolver = filterArgumentResolver;
        this.sortArgumentResolver = sortArgumentResolver;
        this.pageableArgumentResolver = pageableArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(filterArgumentResolver);
        resolvers.add(sortArgumentResolver);
        resolvers.add(pageableArgumentResolver);
    }

    @Bean
    public RestTemplate jsonApiRestTemplate(JsonApiMediaTypeConfiguration jsonApiMediaTypeConfiguration) {
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper();
        
        // Use the explicit bean to configure our client-side ObjectMapper
        jsonApiMediaTypeConfiguration.configureObjectMapper(objectMapper);

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);
        converter.setSupportedMediaTypes(List.of(MediaType.parseMediaType("application/vnd.api+json")));

        restTemplate.getMessageConverters().add(0, converter);
        return restTemplate;
    }

    /**
     * This bean is required to make the JSON:API configuration available for both
     * server-side rendering (via spring.factories) and client-side RestTemplate configuration (via injection).
     */
    @Bean
    public JsonApiMediaTypeConfiguration jsonApiMediaTypeConfiguration(
            ObjectProvider<JsonApiConfiguration> configuration,
            AutowireCapableBeanFactory beanFactory) {
        return new JsonApiMediaTypeConfiguration(configuration, beanFactory);
    }

    /**
     * Provides a custom resolver for generating HATEOAS links (next, prev, etc.)
     * that conform to the JSON:API specification for pagination and sorting parameters.
     * This bean is picked up by the auto-configured PagedResourcesAssembler.
     */
    @Bean
    public HateoasPageableHandlerMethodArgumentResolver hateoasPageableHandlerMethodArgumentResolver() {
        return new JsonApiHateoasPageableHandlerMethodArgumentResolver();
    }

    @Bean
    public PagedResourcesAssembler<?> pagedResourcesAssembler(HateoasPageableHandlerMethodArgumentResolver resolver) {
        return new PagedResourcesAssembler<>(resolver, null);
    }
}