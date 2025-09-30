package dexter.banking.booktransfers.infrastructure.provider;

import dexter.banking.booktransfers.core.domain.shared.context.JourneyContext;
import dexter.banking.booktransfers.core.domain.shared.context.JourneyContextManager;
import dexter.banking.booktransfers.core.domain.shared.context.JourneySpecification;
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
        String journeyName = command.getIdentifier();
        JourneySpecification spec = blueprintProvider.findByName(journeyName)
            .orElseThrow(() -> new IllegalStateException("No journey specification found for command identifier: " + journeyName));

        var context = new JourneyContext(spec);
        try {
            return JourneyContextManager.runWithContext(context, next::invoke);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
