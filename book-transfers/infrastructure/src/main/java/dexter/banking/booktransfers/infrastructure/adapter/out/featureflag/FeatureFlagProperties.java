package dexter.banking.booktransfers.infrastructure.adapter.out.featureflag;

import dexter.banking.booktransfers.core.domain.shared.featureflag.UserGroup;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "feature-flags")
@Getter
@Setter
class FeatureFlagProperties {
    private Map<UserGroup, Set<String>> groupAssignments = Collections.emptyMap();
}
