package dexter.banking.limit.config.repository;

import dexter.banking.limit.config.model.RulesConfig;
import dexter.banking.limit.config.model.ServiceConfig;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ConfigurationRepository {

    public ServiceConfig findServiceConfig(String scheme) {
        if ("SEPA".equals(scheme)) {
            return new ServiceConfig(true, true, "^DE\\d{20}$", true);
        } else {
            // SWIFT
            return new ServiceConfig(true, false, "^[A-Z]{2}\\d{2}[A-Z0-9]{1,30}$", true);
        }
    }

    public RulesConfig findRulesConfig(String scheme) {
        if ("SEPA".equals(scheme)) {
            return new RulesConfig(true, false, Set.of("name", "iban", "address.city", "address.zip", "dob"));
        } else {
            return new RulesConfig(true, true, Set.of("name", "address.city", "address.zip"));
        }
    }
}
