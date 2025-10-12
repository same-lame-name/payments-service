package dexter.banking.booktransfers.core.domain.featureflag;

import java.util.Set;

public record User(String userId, Set<UserGroup> groups) {
    public User(String userId) {
        this(userId, Set.of());
    }
}
