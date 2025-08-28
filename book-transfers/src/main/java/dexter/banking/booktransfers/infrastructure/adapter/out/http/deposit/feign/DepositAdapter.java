package dexter.banking.booktransfers.infrastructure.adapter.out.http.deposit.feign;

import dexter.banking.booktransfers.core.port.DepositPort;
import dexter.banking.model.ApiConstants;
import dexter.banking.model.DepositBankingRequest;
import dexter.banking.model.DepositBankingResponse;
import dexter.banking.model.DepositBankingReversalRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(value = "deposit-banking-service")
public interface DepositAdapter extends DepositPort {

    @Override
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = ApiConstants.API_DEPOSIT_BANKING)
    DepositBankingResponse submitDeposit(@RequestBody DepositBankingRequest reservationRequest);

    @Override
    @PutMapping(
            produces = MediaType.APPLICATION_JSON_VALUE,
            value = ApiConstants.API_DEPOSIT_BANKING + "/{depositRequestId}/cancelled")
    DepositBankingResponse submitDepositReversal(@PathVariable("depositRequestId") UUID depositRequestId,
                                                 @RequestBody DepositBankingReversalRequest depositBankingReversalRequest);
}


