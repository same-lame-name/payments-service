package dexter.banking.booktransfers.infrastructure.adapter.out.featureflag;

import dexter.banking.booktransfers.core.domain.shared.featureflag.UserGroup;
import dexter.banking.booktransfers.core.port.out.NamedGroupProviderPort;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
class ConfigBasedNamedGroupProviderAdapter implements NamedGroupProviderPort {

    private final FeatureFlagProperties featureFlagProperties;
    private final Map<String, Set<UserGroup>> userToGroups;

    public ConfigBasedNamedGroupProviderAdapter(FeatureFlagProperties featureFlagProperties) {
        this.featureFlagProperties = featureFlagProperties;
        this.userToGroups = invertGroupAssignments(featureFlagProperties.getGroupAssignments());
    }

    /**
     * Inverts the group-to-user map from properties into a user-to-group map
     * for efficient lookups. This is a one-time cost at startup.
     */
    private Map<String, Set<UserGroup>> invertGroupAssignments(Map<UserGroup, Set<String>> groupAssignments) {
        if (groupAssignments == null || groupAssignments.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Set<UserGroup>> invertedMap = new HashMap<>();
        groupAssignments.forEach((group, userIds) -> {
            if (userIds != null) {
                userIds.forEach(userId -> {
                    invertedMap.computeIfAbsent(userId, k -> new HashSet<>()).add(group);
                });
            }
        });
        return Collections.unmodifiableMap(invertedMap);
    }

    @Override
    public Set<UserGroup> getGroupsForUser(String userId) {
        return this.userToGroups.getOrDefault(userId, Collections.emptySet());
    }

    @Override
    public Map<UserGroup, Set<String>> getUsersForGroups(Set<UserGroup> groupNames) {
        // This method is used by the cache warmer.
        // It filters the main assignments to return only the data for the requested groups.
        if (featureFlagProperties.getGroupAssignments() == null) {
            return Collections.emptyMap();
        }
        return this.featureFlagProperties.getGroupAssignments()
                .entrySet()
                .stream()
                .filter(entry -> groupNames.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
