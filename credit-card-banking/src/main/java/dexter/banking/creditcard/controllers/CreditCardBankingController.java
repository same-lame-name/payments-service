package dexter.banking.creditcard.controllers;

import dexter.banking.creditcard.model.CreditCardBankingInfo;
import dexter.banking.creditcard.services.CreditCardBankingService;
import dexter.banking.model.ApiConstants;
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
import dexter.banking.model.CreditCardBankingReversalRequest;
import dexter.banking.model.CreditCardBankingRequest;
import dexter.banking.model.CreditCardBankingResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;


@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(ApiConstants.API_CREDIT_CARD_BANKING)
@Validated
public class CreditCardBankingController {

    private final CreditCardBankingService creditCardBankingService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    List<CreditCardBankingInfo> getCars() {
        log.debug("Get all available credit cards");
        return creditCardBankingService.getAll();
    }

    @GetMapping(value = "/{creditCardId}", produces = MediaType.APPLICATION_JSON_VALUE)
    CreditCardBankingInfo getCarInfo(@PathVariable UUID creditCardId) {
        log.debug("Get information for a credit card by Id");
        return creditCardBankingService.getCreditCardBankingInfo(creditCardId);
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    CreditCardBankingResponse submitCreditCardBankingRequest(@RequestBody @Valid CreditCardBankingRequest creditCardBankingRequest) {
        log.debug("Processing credit card banking request: {}", creditCardBankingRequest);
        return creditCardBankingService.submitCreditCardBankingRequest(creditCardBankingRequest);
    }

    @PutMapping(value = "/{creditCardBankingId}/cancelled", produces = MediaType.APPLICATION_JSON_VALUE)
    CreditCardBankingResponse submitCreditCardBankingReversalRequest(@PathVariable @NotNull UUID creditCardBankingId,
                                                                     @RequestBody @Valid CreditCardBankingReversalRequest creditCardBankingReversalRequest) {

        if (!creditCardBankingId.equals(creditCardBankingReversalRequest.getCreditReversalId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        log.debug("Process credit card banking reversal request: {}", creditCardBankingReversalRequest);
        return creditCardBankingService.creditCardBankingReversal(creditCardBankingReversalRequest);
    }
}
