package dexter.banking.limit.config.repository;

import dexter.banking.limit.config.model.RulesConfig;
import dexter.banking.limit.config.model.ServiceConfig;
import org.springframework.stereotype.Component;

@Component
public class ConfigurationRepository {

    public ServiceConfig findServiceConfig(String scheme) {
        if ("SEPA".equals(scheme)) {
            return new ServiceConfig(true, true, "^DE\\d{20}$");
        } else {
            // SWIFT
            return new ServiceConfig(true, false, "^[A-Z]{2}\\d{2}[A-Z0-9]{1,30}$");
        }
    }

    public RulesConfig findRulesConfig(String scheme) {
        if ("SEPA".equals(scheme)) {
            return new RulesConfig(true, false);
        } else {
            // SWIFT - High risk checks required
            return new RulesConfig(true, true);
        }
    }
}