package dexter.banking.booktransfers.core.port.out;


import dexter.banking.booktransfers.core.domain.payment.valueobject.result.CreditLegResult;

import java.util.UUID;

/**
 * Driven Port for interacting with the external Credit Card Banking service.
 * The contract is defined in terms of pure domain objects, protecting the core from
 * the specifics of the external DTOs.
 */
public interface CreditCardPort {
    CreditLegResult submitCreditCardPayment(SubmitCreditCardPaymentRequest request);
    CreditLegResult submitCreditCardReversalPayment(SubmitCreditCardReversalRequest request);

    record SubmitCreditCardPaymentRequest(UUID transactionId, String cardNumber) {}
    record SubmitCreditCardReversalRequest(UUID transactionId, UUID creditCardRequestId) {}
}
