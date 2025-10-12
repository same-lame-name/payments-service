package dexter.banking.booktransfers.infrastructure.provider;

import dexter.banking.booktransfers.core.application.featureflag.UserContextManager;
import dexter.banking.booktransfers.core.application.featureflag.ValidationRuleRegistry;
import dexter.banking.booktransfers.core.application.payment.command.PaymentCommand;
import dexter.banking.booktransfers.core.domain.featureflag.User;
import dexter.banking.booktransfers.core.domain.featureflag.UserGroup;
import dexter.banking.booktransfers.core.domain.featureflag.exception.FeatureDisabledException;
import dexter.banking.booktransfers.core.domain.featureflag.exception.FeatureNotAvailableForUserException;
import dexter.banking.booktransfers.core.domain.shared.context.JourneyContextManager;
import dexter.banking.booktransfers.core.domain.shared.context.JourneySpecification;
import dexter.banking.booktransfers.core.port.out.NamedGroupProviderPort;
import dexter.banking.commandbus.Command;
import dexter.banking.commandbus.Middleware;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Order(3) // Executes after SecurityContextMiddleware (2) and ConfigurationEnrichmentMiddleware (1)
@RequiredArgsConstructor
public class FeatureFlagMiddleware implements Middleware {

    private final UserContextManager userContextManager;
    private final NamedGroupProviderPort namedGroupProvider;
    private final ValidationRuleRegistry validationRuleRegistry;
    private final BlueprintProvider blueprintProvider;


    @Override

    public <R, C extends Command<R>> R invoke(C command, Next<R> next) {
        String journeyName = command.getIdentifier();
        JourneySpecification spec = blueprintProvider.findByName(journeyName)
                .orElseThrow(() -> new IllegalStateException("No journey specification found for command identifier: " + journeyName));

        JourneySpecification.FeatureFlag flag = spec.getFeatureFlag();

        // If the flag is null or has no pilot groups, the journey is public.
        if (flag == null || !flag.enabled() || flag.pilotGroups().isEmpty()) {
            return next.invoke();
        }
        // Get the partial user from the context.

        User partialUser = userContextManager.get();

        // LAZY LOADING: Get the user's groups. This should be a cache hit.
        Set<UserGroup> userGroups = namedGroupProvider.getGroupsForUser(partialUser.userId());

        // Create the fully hydrated user.
        User fullUser = new User(partialUser.userId(), userGroups);

        // EXTENSIBLE VALIDATION: Check if the user satisfies any of the required group rules.
        boolean isAuthorized = flag.pilotGroups().stream()
                .map(validationRuleRegistry::getRuleFor)
                .anyMatch(rule -> rule.isSatisfied(fullUser, spec));

        if (!isAuthorized) {
            throw new FeatureNotAvailableForUserException(spec.getJourneyName(), fullUser.userId());
        }

        // User is authorized, proceed with the chain.
        return next.invoke();
    }
}
