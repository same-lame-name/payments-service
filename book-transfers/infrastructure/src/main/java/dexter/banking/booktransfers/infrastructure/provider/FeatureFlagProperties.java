package dexter.banking.booktransfers.infrastructure.provider;

import dexter.banking.booktransfers.core.domain.featureflag.UserGroup;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "app.feature-flags")
@Getter
@Setter
public class FeatureFlagProperties {
    private Map<UserGroup, Set<String>> groupAssignments = Collections.emptyMap();
}
