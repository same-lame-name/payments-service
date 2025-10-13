package dexter.banking.booktransfers.core.application.middleware;

import dexter.banking.booktransfers.core.domain.shared.context.UserContextManager;
import dexter.banking.booktransfers.core.domain.shared.featureflag.User;
import dexter.banking.commandbus.Command;
import dexter.banking.commandbus.Middleware;
import dexter.banking.commandbus.UserAwareCommand;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
class SecurityContextMiddleware implements Middleware {

    @Override

    public <R, C extends Command<R>> R invoke(C command, Next<R> next) {
        // In a real application, this would come from a JWT or security context.
        // Check if the command is user-aware.
        if (command instanceof UserAwareCommand userAwareCommand) {
            String userId = userAwareCommand.getUserId();

            // If a userId is present, set up the context and execute the rest of the chain within it.
            if (userId != null && !userId.isBlank()) {
                var partialUser = new User(userId);
                return UserContextManager.callWith(partialUser, next::invoke);
            }
        }

        // If the command is not user-aware, or has no user, proceed without context.
        return next.invoke();
    }
}
