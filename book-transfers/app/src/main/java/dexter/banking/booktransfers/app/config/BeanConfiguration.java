package config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dexter.banking.commandbus.CommandBus;
import dexter.banking.commandbus.CommandDispatcher;
import dexter.banking.commandbus.CommandHandler;
import dexter.banking.commandbus.Middleware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

@Configuration
public class BeanConfiguration {

    @Bean
    public CommandBus commandBus(List<CommandHandler<?, ?>> commandHandlers,
                                 List<Middleware> middlewares) {
        return CommandDispatcher.builder()
                .withMiddlewares(middlewares) // Spring provides this list pre-ordered
                .withCommandHandlers(commandHandlers)
                .build();
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }
}
