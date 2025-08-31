package dexter.banking.booktransfers.core.domain.model.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A request-scoped carrier for data that needs to be passed between middlewares.
 * It now holds a pure domain JourneySpecification object, removing any dependency on infrastructure.
 */
@RequiredArgsConstructor
@Getter
public class CommandProcessingContext {
    private final JourneySpecification journeySpecification;
    // This context can be extended with more fields in the future.
}
