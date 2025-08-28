package dexter.banking.booktransfers.core.middleware.context;

import dexter.banking.booktransfers.core.domain.model.config.CommandConfiguration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A request-scoped carrier for data that needs to be passed between middlewares.
 * It now holds a pure domain CommandConfiguration object, removing any dependency on infrastructure.
 */
@RequiredArgsConstructor
@Getter
public class CommandProcessingContext {
    private final CommandConfiguration commandConfiguration;
    // This context can be extended with more fields in the future.
}
