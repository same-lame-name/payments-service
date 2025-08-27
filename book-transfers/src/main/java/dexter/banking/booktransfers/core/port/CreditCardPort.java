package dexter.banking.booktransfers.core.port;

import dexter.banking.model.CreditCardBankingRequest;
import dexter.banking.model.CreditCardBankingResponse;
import dexter.banking.model.CreditCardBankingReversalRequest;
import java.util.UUID;

/**
 * Driven Port for interacting with the external Credit Card Banking service.
 * The infrastructure layer will provide an adapter (e.g., a Feign client) to implement this contract.
 */
public interface CreditCardPort {
    CreditCardBankingResponse submitCreditCardPayment(CreditCardBankingRequest request);
    CreditCardBankingResponse submitCreditCardReversalPayment(UUID creditCardRequestId, CreditCardBankingReversalRequest request);
}


