package dexter.banking.booktransfers.infrastructure.adapter.out.http.payment.feign;

import dexter.banking.booktransfers.core.application.payment.command.PaymentCommand;
import dexter.banking.booktransfers.core.domain.payment.Payment;
import dexter.banking.booktransfers.core.domain.payment.result.DebitLegResult;
import dexter.banking.booktransfers.core.port.out.DepositPort;
import dexter.banking.model.ApiConstants;
import dexter.banking.model.DepositBankingRequest;
import dexter.banking.model.DepositBankingResponse;
import dexter.banking.model.DepositBankingReversalRequest;
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
@Component
@Primary
class DepositAdapter implements DepositPort {

    private final RawDepositClient client;
    private final HttpAdapterMapper mapper;
    @Autowired
    public DepositAdapter(RawDepositClient client, HttpAdapterMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    public DebitLegResult submitDeposit(PaymentCommand command) {
        DepositBankingRequest request = mapper.toDepositBankingRequest(command);
        DepositBankingResponse responseDto = client.submitDeposit(request);
        return mapper.toDomain(responseDto);
    }

    @Override
    public DebitLegResult submitDepositReversal(Payment payment) {
        DepositBankingReversalRequest request = mapper.toDepositReversalRequest(payment);
        DepositBankingResponse responseDto = client.submitDepositReversal(payment.getDebitLegResult().depositId(), request);
        return mapper.toReversalDomain(responseDto);
    }


    @FeignClient(value = "deposit-banking-service")
    interface RawDepositClient {

        @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = ApiConstants.API_DEPOSIT_BANKING)
        DepositBankingResponse submitDeposit(@RequestBody DepositBankingRequest reservationRequest);
        @PutMapping(
                produces = MediaType.APPLICATION_JSON_VALUE,
                value = ApiConstants.API_DEPOSIT_BANKING + "/{depositRequestId}/cancelled")
        DepositBankingResponse submitDepositReversal(@PathVariable("depositRequestId") UUID depositRequestId,
                                                     @RequestBody DepositBankingReversalRequest depositBankingReversalRequest);
    }
}
