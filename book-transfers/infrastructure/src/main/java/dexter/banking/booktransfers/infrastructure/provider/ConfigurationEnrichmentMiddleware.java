package dexter.banking.booktransfers.infrastructure.provider;

import dexter.banking.booktransfers.core.domain.shared.context.JourneySpecification;
import dexter.banking.commandbus.JourneyAwareCommand;
import dexter.banking.commandbus.Command;
import dexter.banking.commandbus.Middleware;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(1)
@Component
@RequiredArgsConstructor
class ConfigurationEnrichmentMiddleware implements Middleware {

    private final BlueprintProvider blueprintProvider;

    @Override
    public <R, C extends Command<R>> R invoke(C command, Next<R> next) {

        // New logic: Check if the command is journey-aware
        if (command instanceof JourneyAwareCommand) {
            String journeyName = command.getIdentifier();
            JourneySpecification spec = blueprintProvider.findByName(journeyName)
                    .orElseThrow(() -> new IllegalStateException("No journey specification found for command identifier: " + journeyName));

            // Attach the blueprint directly to the command
            ((JourneyAwareCommand<?>) command).setBlueprint(spec.getBlueprint());
        }

        return next.invoke();
    }
}
