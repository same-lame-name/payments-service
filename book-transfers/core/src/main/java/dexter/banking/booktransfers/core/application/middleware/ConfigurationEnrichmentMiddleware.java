package dexter.banking.booktransfers.core.application.middleware;

import dexter.banking.booktransfers.core.domain.shared.context.JourneyContext;
import dexter.banking.booktransfers.core.domain.shared.context.JourneyContextManager;
import dexter.banking.booktransfers.core.domain.shared.context.JourneySpecification;
import dexter.banking.booktransfers.core.port.out.ConfigurationPort;
import dexter.banking.commandbus.Command;
import dexter.banking.commandbus.Middleware;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Order(1)
@Component
@RequiredArgsConstructor
@Slf4j
public class ConfigurationEnrichmentMiddleware implements Middleware {

    private final ConfigurationPort configurationPort;

    @Override
    public <R, C extends Command<R>> R invoke(C command, Next<R> next) {
        String journeyName = command.getIdentifier();
        var journeySpecification = configurationPort
                .findForJourney(journeyName)
                .orElseGet(JourneySpecification::defaultInstance);

        var context = new JourneyContext(journeySpecification);

        log.debug("Running command for journey '{}' with ScopedValue journey context", journeyName);

        try {
            // This logic correctly handles the `throws Exception` signature of `runWithContext`
            // while respecting the `invoke` method's signature, which does not throw checked exceptions.
            return JourneyContextManager.runWithContext(context, next::invoke);
        } catch (RuntimeException e) {
            // Propagate runtime exceptions from the downstream middleware/handlers.
            throw e;
        } catch (Exception e) {
            // Wrap any unexpected checked exceptions from `runWithContext` itself.
            throw new RuntimeException(e);
        }
    }
}
