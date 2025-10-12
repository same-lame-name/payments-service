package dexter.banking.booktransfers.core.application.featureflag;

import dexter.banking.booktransfers.core.domain.featureflag.User;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class UserContextManager {
    private static final ScopedValue<User> user = ScopedValue.newInstance();

    public void runWith(User user, Runnable runnable) {
        ScopedValue.where(UserContextManager.user, user).run(runnable);
    }

    public static <R> R callWith(User user, Supplier<R> supplier) {
        try {
            return ScopedValue.where(UserContextManager.user, user).call(supplier::get);
        } catch (Exception e) {
            // It's generally better to let the specific middleware or handler deal with exceptions.
            // Wrapping in a RuntimeException simplifies the method signature for the middleware.
            throw new RuntimeException(e);
        }
    }

    public User get() {
        // Return null if not bound, to avoid ScopedValue throwing an exception
        // when no user context is set (e.g., for non-user-aware commands).
        if (!user.isBound()) {
            return null;
        }
        return user.get();
    }
}
