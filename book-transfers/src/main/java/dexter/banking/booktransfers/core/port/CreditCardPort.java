package dexter.banking.booktransfers.core.port;

import dexter.banking.booktransfers.core.domain.model.results.CreditLegResult;
import dexter.banking.model.CreditCardBankingRequest;
import dexter.banking.model.CreditCardBankingReversalRequest;
import java.util.UUID;

/**
 * Driven Port for interacting with the external Credit Card Banking service.
 * The contract is defined in terms of pure domain objects, protecting the core from
 * the specifics of the external DTOs.
 */
public interface CreditCardPort {
    CreditLegResult submitCreditCardPayment(CreditCardBankingRequest request);
    CreditLegResult submitCreditCardReversalPayment(UUID creditCardRequestId, CreditCardBankingReversalRequest request);
}
