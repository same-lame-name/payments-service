package dexter.banking.limit.model;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

@Value
@Builder
public class LimitManagementInfo {

    UUID id;
    String limitType;
    String accountType;
    BigDecimal limitAmount;
    CustomerType customerType;
    boolean available;
}
