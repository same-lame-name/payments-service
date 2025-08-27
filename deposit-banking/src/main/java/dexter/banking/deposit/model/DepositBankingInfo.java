package dexter.banking.deposit.model;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

@Value
@Builder
public class DepositBankingInfo {

    UUID id;
    String accountNumber;
    String accountName;
    BigDecimal balance;
    boolean available;
}
