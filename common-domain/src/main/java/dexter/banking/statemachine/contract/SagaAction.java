package dexter.banking.statemachine.contract;
import java.util.Optional;

/**
 * A contract for a compensatable action within a Saga pattern.
 * <p>
 * This interface bundles the forward operation (apply) with its corresponding
 * compensating/rollback operation (compensate). This creates a highly cohesive
 * unit for managing steps in a distributed transaction.
 *
 * @param <S> The State enum type
 * @param <E> The Event enum type
 * @param <C> The Context object type
 */
public interface SagaAction<S, E, C extends StateMachineContext<S>> {

    /**
     * The forward operation. Executes the primary business logic for this step.
     *
     * @param context The state machine context.
     * @param event The event that triggered this transition.
     * @return An Optional event to cascade, typically reporting success or failure.
     */
    Optional<E> apply(C context, E event);

    /**
     * The compensating operation. Reverts or compensates for the work done by the apply() method.
     *
     * @param context The state machine context.
     * @param event The event that triggered this transition.
     * @return An Optional event to cascade, typically reporting the success or failure of the reversal.
     */
    Optional<E> compensate(C context, E event);
}
