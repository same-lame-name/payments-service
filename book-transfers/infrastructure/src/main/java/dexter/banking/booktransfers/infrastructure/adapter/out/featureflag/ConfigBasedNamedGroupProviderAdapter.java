package dexter.banking.booktransfers.infrastructure.adapter.out.featureflag;

import dexter.banking.booktransfers.core.domain.featureflag.UserGroup;
import dexter.banking.booktransfers.core.port.out.NamedGroupProviderPort;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class ConfigBasedNamedGroupProviderAdapter implements NamedGroupProviderPort {
    // This adapter would typically load from a config file or a database.
    // For this MVP, we will use a hardcoded map to represent the data source.
    private static final Map<String, Set<UserGroup>> USER_DATA = Map.of(
            "user-1234", Set.of(UserGroup.BETA_TESTERS_WAVE_1),
            "user-456", Set.of(UserGroup.INTERNAL_AUDITORS, UserGroup.APP_V2_USERS)
    );

    @Override
    public Set<UserGroup> getGroupsForUser(String userId) {
        return USER_DATA.getOrDefault(userId, Set.of());
    }

    @Override
    public Map<UserGroup, Set<String>> getUsersForGroups(Set<UserGroup> groupNames) {
        // This would be an inverse lookup from the data source.
        // Later to be moved to a cache.
        return Map.of(); // Placeholder for now
    }
}
