package dexter.banking.limit.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toedter.spring.hateoas.jsonapi.JsonApiConfiguration;
import com.toedter.spring.hateoas.jsonapi.JsonApiMediaTypeConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
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

    private final JsonApiArgumentResolver jsonApiArgumentResolver;

    public JsonApiConfig(JsonApiArgumentResolver jsonApiArgumentResolver) {
        this.jsonApiArgumentResolver = jsonApiArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        // Resolver for @JsonApiQuery DomainQuery
        resolvers.add(jsonApiArgumentResolver);

        // Resolver for standalone Pageable (legacy/simple support)
        JsonApiPageableResolver pageResolver = new JsonApiPageableResolver();
        pageResolver.setPageParameterName("page[number]");
        pageResolver.setSizeParameterName("page[size]");
        pageResolver.setOneIndexedParameters(true); // JSON:API uses 1-based indexing
        pageResolver.setFallbackPageable(PageRequest.of(0, 10)); // Default to page 1 (index 0), size 10
        resolvers.add(pageResolver);
    }

    @Bean
    public RestTemplate jsonApiRestTemplate(JsonApiMediaTypeConfiguration jsonApiMediaTypeConfiguration) {
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper();
        jsonApiMediaTypeConfiguration.configureObjectMapper(objectMapper);

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);
        converter.setSupportedMediaTypes(List.of(MediaType.parseMediaType("application/vnd.api+json")));

        restTemplate.getMessageConverters().add(0, converter);
        return restTemplate;
    }

    @Bean
    public JsonApiMediaTypeConfiguration jsonApiMediaTypeConfiguration(
            ObjectProvider<JsonApiConfiguration> configuration,
            AutowireCapableBeanFactory beanFactory) {
        return new JsonApiMediaTypeConfiguration(configuration, beanFactory);
    }

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> factory.addConnectorCustomizers(connector -> {
            connector.setProperty("relaxedQueryChars", "[]");
        });
    }

    @Bean
    public HateoasPageableHandlerMethodArgumentResolver hateoasPageableHandlerMethodArgumentResolver() {
        return new JsonApiHateoasPageableHandlerMethodArgumentResolver();
    }

    @Bean
    public PagedResourcesAssembler<?> pagedResourcesAssembler(HateoasPageableHandlerMethodArgumentResolver resolver) {
        return new PagedResourcesAssembler<>(resolver, null);
    }
}