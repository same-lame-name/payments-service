package dexter.banking.booktransfers.infrastructure.adapter.in.middleware;

import dexter.banking.booktransfers.core.middleware.context.CommandProcessingContext;
import dexter.banking.booktransfers.core.middleware.context.CommandProcessingContextHolder;
import dexter.banking.booktransfers.core.port.ConfigurationPort;
import dexter.banking.commandbus.Command;
import dexter.banking.commandbus.Middleware;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * An inbound adapter that acts as a Middleware.
 * Its responsibility is to fetch configuration via the ConfigurationPort and enrich
 * the current request context, making it available to downstream core components.
 */
@Order(1)
@Component
@RequiredArgsConstructor
@Slf4j
public class ConfigurationEnrichmentMiddleware implements Middleware {

    private final ConfigurationPort configurationPort;

    @Override
    public <R, C extends Command<R>> R invoke(C command, Next<R> next) {
        String identifier = command.getIdentifier();

        // This middleware now correctly depends on a core port, not an infrastructure detail.
        configurationPort.findForCommand(identifier).ifPresent(config -> {
            CommandProcessingContext context = new CommandProcessingContext(config);
            CommandProcessingContextHolder.setContext(context);
            log.debug("Service config for '{}' loaded into context.", identifier);
        });

        if (CommandProcessingContextHolder.getContext().isEmpty()) {
            log.warn("No service configuration found for command identifier: '{}'. Proceeding without configuration.", identifier);
        }

        try {
            return next.invoke();
        } finally {
            // CRUCIAL: Always clear the context to prevent memory leaks in a threaded environment.
            if (CommandProcessingContextHolder.getContext().isPresent()) {
                CommandProcessingContextHolder.clearContext();
                log.debug("Context cleared for command identifier: '{}'", identifier);
            }
        }
    }
}
