package dexter.banking.booktransfers.core.domain.shared.blueprint;

import dexter.banking.booktransfers.core.domain.shared.context.JourneyContextManager;
import org.springframework.stereotype.Component;

@Component
class ContextBasedBlueprintAccessor implements BlueprintAccessor {
    @Override
    @SuppressWarnings("unchecked")
    public <T extends JourneyBlueprint> T get() {
        JourneyBlueprint blueprint = JourneyContextManager.getContext().getSpecification().getBlueprint();
        // The cast is safe because the type is inferred from the calling code's
        // variable declaration, which is what the developer intends.
        return (T) blueprint;
    }

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

