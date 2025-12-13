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
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.data.web.HateoasSortHandlerMethodArgumentResolver;
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
        
        jsonApiMediaTypeConfiguration.configureObjectMapper(objectMapper);

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);
        converter.setSupportedMediaTypes(List.of(MediaType.parseMediaType(JsonApiConstants.MEDIA_TYPE)));

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
    public HateoasSortHandlerMethodArgumentResolver jsonApiHateoasSortHandlerMethodArgumentResolver() {
        return new JsonApiHateoasSortHandlerMethodArgumentResolver();
    }

    @Bean
    public HateoasPageableHandlerMethodArgumentResolver hateoasPageableHandlerMethodArgumentResolver(
            HateoasSortHandlerMethodArgumentResolver sortResolver) {
        return new JsonApiHateoasPageableHandlerMethodArgumentResolver(sortResolver);
    }

    @Bean
    public PagedResourcesAssembler<?> pagedResourcesAssembler(HateoasPageableHandlerMethodArgumentResolver resolver) {
        return new PagedResourcesAssembler<>(resolver, null);
    }

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> factory.addConnectorCustomizers(connector -> {
            connector.setProperty("relaxedQueryChars", "[]");
        });
    }
}