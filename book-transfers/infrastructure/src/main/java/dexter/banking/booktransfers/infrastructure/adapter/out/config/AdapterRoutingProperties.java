package dexter.banking.booktransfers.infrastructure.adapter.out.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdapterRoutingProperties {
    private String depositPort;
    private String creditCardPort;
    private String limitPort;
    private String transactionLegPort;
}
