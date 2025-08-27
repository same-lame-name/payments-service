package dexter.banking.booktransfers.core.middleware;

import dexter.banking.booktransfers.core.middleware.context.CommandProcessingContext;
import dexter.banking.booktransfers.core.middleware.context.CommandProcessingContextHolder;
import dexter.banking.booktransfers.infrastructure.config.model.CommandConfig;
import dexter.banking.booktransfers.infrastructure.config.model.ServiceConfigProperties;
import dexter.banking.commandbus.Command;
import dexter.banking.commandbus.Middleware;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(1)
@Component
@RequiredArgsConstructor
@Slf4j
public class ConfigurationEnrichmentMiddleware implements Middleware {

    private final ServiceConfigProperties serviceConfigProperties;
    @Override
    public <R, C extends Command<R>> R invoke(C command, Next<R> next) {
        String identifier = command.getIdentifier();
        CommandConfig config = serviceConfigProperties.getCommands().get(identifier);

        if (config == null) {
            log.warn("No service configuration found for command identifier: '{}'. Proceeding without configuration.", identifier);
            // Even if no config is found, we proceed, but without a context.
            return next.invoke();
        }

        try {
            CommandProcessingContext context = new CommandProcessingContext(config);
            CommandProcessingContextHolder.setContext(context);
            log.debug("Service config for '{}' loaded into context.", identifier);
            return next.invoke();
        } finally {
            // CRUCIAL: Always clear the context to prevent memory leaks in a threaded environment.
            CommandProcessingContextHolder.clearContext();
            log.debug("Context cleared for command identifier: '{}'", identifier);
        }
    }
}
