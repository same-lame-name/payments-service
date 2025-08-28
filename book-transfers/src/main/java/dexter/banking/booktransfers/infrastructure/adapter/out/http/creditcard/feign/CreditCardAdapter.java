package dexter.banking.booktransfers.infrastructure.adapter.out.http.creditcard.feign;

import dexter.banking.booktransfers.core.domain.model.results.CreditLegResult;
import dexter.banking.booktransfers.core.port.CreditCardPort;
import dexter.banking.booktransfers.infrastructure.adapter.out.http.mapper.HttpAdapterMapper;
import dexter.banking.model.ApiConstants;
import dexter.banking.model.CreditCardBankingRequest;
import dexter.banking.model.CreditCardBankingResponse;
import dexter.banking.model.CreditCardBankingReversalRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

/**
 * Feign client adapter that implements the CreditCardPort.
 * This is the concrete implementation that knows how to talk to the external service.
 * It is composed of a raw Feign client and a mapper to act as an Anti-Corruption Layer.
 */
@Component
@Primary
public class CreditCardAdapter implements CreditCardPort {

    private final RawCreditCardClient client;
    private final HttpAdapterMapper mapper;

    @Autowired
    public CreditCardAdapter(RawCreditCardClient client, HttpAdapterMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    public CreditLegResult submitCreditCardPayment(CreditCardBankingRequest request) {
        CreditCardBankingResponse responseDto = client.submitCreditCardPayment(request);
        return mapper.toDomain(responseDto);
    }

    @Override
    public CreditLegResult submitCreditCardReversalPayment(UUID creditCardRequestId, CreditCardBankingReversalRequest request) {
        CreditCardBankingResponse responseDto = client.submitCreditCardReversalPayment(creditCardRequestId, request);
        // We can reuse the same mapping as the response structure is the same.
        return mapper.toDomain(responseDto);
    }

    @FeignClient(value = "credit-card-banking-service")
    interface RawCreditCardClient {
        @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = ApiConstants.API_CREDIT_CARD_BANKING)
        CreditCardBankingResponse submitCreditCardPayment(@RequestBody CreditCardBankingRequest creditCardBankingRequest);

        @PutMapping(
                produces = MediaType.APPLICATION_JSON_VALUE,
                value = ApiConstants.API_CREDIT_CARD_BANKING + "/{creditCardRequestId}/cancelled")
        CreditCardBankingResponse submitCreditCardReversalPayment(@PathVariable("creditCardRequestId") UUID creditCardRequestId,
                                                                  @RequestBody CreditCardBankingReversalRequest creditCardBankingReversalRequest);
    }
}
