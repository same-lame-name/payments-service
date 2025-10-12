package dexter.banking.booktransfers.infrastructure.provider;

import dexter.banking.booktransfers.core.application.featureflag.UserContextManager;
import dexter.banking.booktransfers.core.domain.featureflag.User;
import dexter.banking.commandbus.Command;
import dexter.banking.commandbus.Middleware;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class SecurityContextMiddleware implements Middleware {

    @Override

    public <R, C extends Command<R>> R invoke(C command, Next<R> next) {
        // In a real application, this would come from a JWT or security context.
        // For this MVP, we will hardcode a userId.
        String userId = "user-123";

        // Create a PARTIALLY hydrated user object (no groups yet).
        var partialUser = new User(userId);

        // Run the rest of the command chain within this user's context.
        try {
            return UserContextManager.runWithUser(partialUser, next::invoke);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
