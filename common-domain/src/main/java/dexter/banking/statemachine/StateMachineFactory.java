package dexter.banking.statemachine;

import dexter.banking.statemachine.contract.StateMachineContext;
import lombok.RequiredArgsConstructor;
import java.util.Optional;

/**
 * A factory for creating and acquiring StateMachine instances.
 * This class uses a StateMachineConfig to construct machines.
 */
@RequiredArgsConstructor
public class StateMachineFactory<S, E, C extends StateMachineContext<S>> {

    private final StateMachineConfig<S, E, C> config;

    /**
     * Gets a new state machine instance for a given context.
     * @param context The context object for the machine to manage.
     * @return A new StateMachine instance.
     */
    public StateMachine<S, E, C> acquireStateMachine(C context) {
        return new StateMachine<>(context, config);
    }

    /**
     * Acquires a state machine by its unique ID, rehydrating it from persistence.
     * It retrieves the persister from the config object.
     * @param machineId The unique identifier of the context.
     * @return An Optional containing the rehydrated state machine, or empty if not found.
     */
    public Optional<StateMachine<S, E, C>> acquireStateMachine(String machineId) {
        // Gets the persister from the config, ensuring no redundancy.
        return config.getPersister()
                .flatMap(persister -> persister.findContextById(machineId))
                .map(context -> new StateMachine<>(context, config));
    }
}
