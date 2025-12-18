package dexter.banking.limit.pipeline.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dexter.banking.limit.config.model.RulesConfig;
import dexter.banking.limit.config.model.ServiceConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;

@Data
@EqualsAndHashCode(callSuper = false)
public abstract class BaseRequest<T extends BaseRequest<T>> extends RepresentationModel<T> {

    @JsonIgnore
    private ServiceConfig serviceConfig;

    @JsonIgnore
    private RulesConfig rulesConfig;

    @JsonIgnore
    private String idempotencyKey;

    public abstract String getJourneyIdentifier();
}