package dexter.banking.booktransfers.core.application.featureflag;

import dexter.banking.booktransfers.core.domain.featureflag.User;
import dexter.banking.booktransfers.core.domain.shared.context.JourneyContext;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class UserContextManager {
    private static final ScopedValue<User> user = ScopedValue.newInstance();

    public void runWith(User user, Runnable runnable) {
        ScopedValue.where(UserContextManager.user, user).run(runnable);
    }

    /**
     * Executes a standard Supplier within a context scope.
     * @param user The User to set for the operation.
     * @param operation The operation to execute.
     * @return The result of the operation.
     */
    public static <R> R runWithUser(User user, Supplier<R> operation) throws Exception {
        return ScopedValue.where(UserContextManager.user, user).call(operation::get);
    }

    public User get() {
        return user.get();
    }
}
