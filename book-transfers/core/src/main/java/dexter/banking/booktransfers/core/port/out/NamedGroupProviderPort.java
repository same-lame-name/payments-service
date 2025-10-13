package dexter.banking.booktransfers.core.port.out;

import dexter.banking.booktransfers.core.domain.shared.featureflag.UserGroup;

import java.util.Map;
import java.util.Set;

public interface NamedGroupProviderPort {
    Set<UserGroup> getGroupsForUser(String userId);
    Map<UserGroup, Set<String>> getUsersForGroups(Set<UserGroup> groupNames);
}
