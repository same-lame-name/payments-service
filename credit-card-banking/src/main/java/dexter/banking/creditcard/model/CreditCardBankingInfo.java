package dexter.banking.creditcard.model;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

@Value
@Builder
public class CreditCardBankingInfo {

    UUID id;
    String cardNumber;
    String fullName;
    BigDecimal balanceDue;
    CardType cardType;
    BigDecimal limit;
    boolean available;
}
