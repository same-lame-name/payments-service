package dexter.banking.limit.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.util.List;

@Component
public class DebugConverters implements CommandLineRunner {

    private final RequestMappingHandlerAdapter adapter;

    public DebugConverters(RequestMappingHandlerAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== DEBUGGING MESSAGE CONVERTERS ===");
        List<HttpMessageConverter<?>> converters = adapter.getMessageConverters();
        for (int i = 0; i < converters.size(); i++) {
            HttpMessageConverter<?> converter = converters.get(i);
            System.out.println("Index " + i + ": " + converter.getClass().getSimpleName());
            if (converter instanceof MappingJackson2HttpMessageConverter) {
                MappingJackson2HttpMessageConverter jacksonConverter = (MappingJackson2HttpMessageConverter) converter;
                System.out.println("  Supported Media Types: " + jacksonConverter.getSupportedMediaTypes());
                ObjectMapper mapper = jacksonConverter.getObjectMapper();
                System.out.println("  Modules: " + mapper.getRegisteredModuleIds());
                
                // TEST SERIALIZATION
                try {
                    String json = mapper.writeValueAsString(java.time.LocalDate.now());
                    System.out.println("  Test LocalDate Serialization: " + json);
                } catch (Exception e) {
                    System.out.println("  Test LocalDate Serialization FAILED: " + e.getMessage());
                }
            }
        }
        System.out.println("====================================");
    }
}