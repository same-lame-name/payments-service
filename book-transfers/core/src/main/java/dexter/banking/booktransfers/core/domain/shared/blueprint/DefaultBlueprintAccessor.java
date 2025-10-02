package dexter.banking.booktransfers.core.domain.shared.blueprint;

import dexter.banking.booktransfers.core.domain.shared.context.JourneyContextManager;
import org.springframework.stereotype.Component;

@Component
class DefaultBlueprintAccessor implements BlueprintAccessor {

    @Override
    public <T extends JourneyBlueprint> T get(Class<T> blueprintType) {
        JourneyBlueprint blueprint = JourneyContextManager.getContext().getSpecification().getBlueprint();

        if (!blueprintType.isInstance(blueprint)) {
            throw new ClassCastException(String.format(
                    "The current journey's blueprint (type: %s) cannot be cast to the requested type: %s",
                    blueprint.getClass().getInterfaces()[0].getSimpleName(),
                    blueprintType.getSimpleName()
            ));
        }
        return blueprintType.cast(blueprint);
    }
}

