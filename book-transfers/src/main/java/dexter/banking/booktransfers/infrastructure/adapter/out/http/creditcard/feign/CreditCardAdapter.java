package dexter.banking.booktransfers.infrastructure.adapter.out.http.creditcard.feign;

import dexter.banking.booktransfers.core.port.CreditCardPort;
import dexter.banking.model.ApiConstants;
import dexter.banking.model.CreditCardBankingRequest;
import dexter.banking.model.CreditCardBankingResponse;
import dexter.banking.model.CreditCardBankingReversalRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

/**
 * Feign client adapter that implements the CreditCardPort.
 * This is the concrete implementation that knows how to talk to the external service.
 */
@FeignClient(value = "credit-card-banking-service")
public interface CreditCardAdapter extends CreditCardPort {

    @Override
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = ApiConstants.API_CREDIT_CARD_BANKING)
    CreditCardBankingResponse submitCreditCardPayment(@RequestBody CreditCardBankingRequest creditCardBankingRequest);

    @Override
    @PutMapping(
            produces = MediaType.APPLICATION_JSON_VALUE,
            value = ApiConstants.API_CREDIT_CARD_BANKING + "/{creditCardRequestId}/cancelled")
    CreditCardBankingResponse submitCreditCardReversalPayment(@PathVariable("creditCardRequestId") UUID creditCardRequestId,
                                                              @RequestBody CreditCardBankingReversalRequest creditCardBankingReversalRequest);
}


