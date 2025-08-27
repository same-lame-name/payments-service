package dexter.banking.booktransfers.core.middleware.context;

import dexter.banking.booktransfers.infrastructure.config.model.CommandConfig;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A request-scoped carrier for data that needs to be passed between middlewares.
 */
@RequiredArgsConstructor
@Getter
public class CommandProcessingContext {
    private final CommandConfig serviceConfig;
    // This context can be extended with more fields in the future.
}
