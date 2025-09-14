package dexter.banking.booktransfers.core.application.middleware;

import dexter.banking.booktransfers.core.domain.shared.config.CommandProcessingContext;
import dexter.banking.booktransfers.core.domain.shared.config.CommandProcessingContextHolder;
import dexter.banking.booktransfers.core.domain.shared.config.JourneySpecification;
import dexter.banking.booktransfers.core.domain.shared.context.JourneyContextManager; // <-- New import
// import dexter.banking.booktransfers.core.port.out.ConfigurationPort; // <-- REMOVED
import dexter.banking.commandbus.Command;
import dexter.banking.commandbus.Middleware;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * An inbound adapter that acts as a Middleware.
 * Its responsibility is now to bridge the new JourneyContextManager to the legacy
 * CommandProcessingContextHolder for downstream compatibility.
 */
@Order(1)
@Component
@RequiredArgsConstructor
@Slf4j
public class ConfigurationEnrichmentMiddleware implements Middleware {

    // private final ConfigurationPort configurationPort; // <-- REMOVED

    @Override
    public <R, C extends Command<R>> R invoke(C command, Next<R> next) {
        String identifier = command.getIdentifier();
        
        // The new logic: Get the spec from the new context manager.
        try {
            JourneySpecification spec = JourneyContextManager.getContext().specification();
            CommandProcessingContext context = new CommandProcessingContext(spec);
            CommandProcessingContextHolder.setContext(context);
            log.debug("Service config for '{}' loaded into legacy context from JourneyContextManager.", identifier);
        } catch (IllegalStateException e) {
            // This occurs if a journey has not yet been migrated to @BeginJourney.
            // We log and proceed without the legacy context, maintaining backward compatibility.
            log.warn("No JourneyContext found for command identifier: '{}'. Proceeding without configuration in legacy context.", identifier);
        }

        try {
            return next.invoke();
        } finally {
            // CRUCIAL: The old context holder still needs to be cleared for thread safety.
            if (CommandProcessingContextHolder.getContext().isPresent()) {
                CommandProcessingContextHolder.clearContext();
                log.debug("Legacy context cleared for command identifier: '{}'", identifier);
            }
        }
    }
}
