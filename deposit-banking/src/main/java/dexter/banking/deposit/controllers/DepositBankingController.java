package dexter.banking.deposit.controllers;

import dexter.banking.deposit.services.DepositBankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import dexter.banking.model.DepositBankingReversalRequest;
import dexter.banking.deposit.model.DepositBankingInfo;
import dexter.banking.model.DepositBankingRequest;
import dexter.banking.model.DepositBankingResponse;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

import static dexter.banking.model.ApiConstants.API_DEPOSIT_BANKING;


@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(API_DEPOSIT_BANKING)
@Validated
public class DepositBankingController {

    private final DepositBankingService depositBankingService;

    @GetMapping
    List<DepositBankingInfo> getDepositBankingInfo() {
        log.debug("Get all deposit banking info");
        return depositBankingService.getAll();
    }

    @GetMapping(value = "/{depositBankingId}", produces = MediaType.APPLICATION_JSON_VALUE)
    DepositBankingInfo getHotelInfo(@PathVariable UUID depositBankingId) {
        return depositBankingService.getDepositBankingInfo(depositBankingId);
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    DepositBankingResponse submitDepositBankingRequest(@RequestBody @Valid DepositBankingRequest depositBankingRequest) {
        log.debug("Submit Deposit banking request: {}", depositBankingRequest);
        return depositBankingService.submitDepositBankingRequest(depositBankingRequest);
    }

    @PutMapping(value = "/{depositBankingId}/cancelled", produces = MediaType.APPLICATION_JSON_VALUE)
    DepositBankingResponse submitDepositBankingReversalRequest(@PathVariable @NotNull UUID depositBankingId,
                                                               @RequestBody @Valid DepositBankingReversalRequest depositBankingReversalRequest) {

        if (!depositBankingId.equals(depositBankingReversalRequest.getReservationId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        log.debug("Submit Deposit banking reversal request: {}", depositBankingReversalRequest);
        return depositBankingService.submitDepositBankingReversal(depositBankingReversalRequest);
    }
}
