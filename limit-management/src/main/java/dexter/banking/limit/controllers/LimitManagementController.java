package dexter.banking.limit.controllers;

import dexter.banking.limit.services.LimitManagementService;
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
import dexter.banking.model.LimitManagementReversalRequest;
import dexter.banking.limit.model.LimitManagementInfo;
import dexter.banking.model.LimitManagementRequest;
import dexter.banking.model.LimitManagementResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

import static dexter.banking.model.ApiConstants.API_LIMIT_MANAGEMENT;


@Slf4j
@RequiredArgsConstructor
@RestController
@Validated
@RequestMapping(API_LIMIT_MANAGEMENT)
public class LimitManagementController {

    private final LimitManagementService limitManagementService;

    @GetMapping
    List<LimitManagementInfo> getAllLimitManagementInfo() {
        log.debug("Get all limit management info");
        return limitManagementService.getAll();
    }

    @GetMapping(value = "/{limitManagementId}", produces = MediaType.APPLICATION_JSON_VALUE)
    LimitManagementInfo getLimitManagementInfo(@PathVariable UUID limitManagementId) {
        return limitManagementService.getLimitManagementInfo(limitManagementId);
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    LimitManagementResponse submitLimitManagementRequest(@RequestBody @Valid LimitManagementRequest limitManagementRequest) {
        log.debug("Submit Limit management request: {}", limitManagementRequest);
        return limitManagementService.submitLimitManagementRequest(limitManagementRequest);
    }

    @PutMapping(value = "/{limitManagementId}/cancelled", produces = MediaType.APPLICATION_JSON_VALUE)
    LimitManagementResponse cancel(@PathVariable @NotNull UUID limitManagementId,
                                   @RequestBody @Valid LimitManagementReversalRequest cancellationRequest) {

        if (!limitManagementId.equals(cancellationRequest.getLimitManagementId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        log.debug("Submit Limit management reversal request: {}", cancellationRequest);
        return limitManagementService.limitManagementReversalRequest(cancellationRequest);
    }
}
