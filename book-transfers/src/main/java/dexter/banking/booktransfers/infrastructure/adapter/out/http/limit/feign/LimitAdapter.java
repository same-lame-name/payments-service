package dexter.banking.booktransfers.infrastructure.adapter.out.http.limit.feign;

import dexter.banking.booktransfers.core.port.LimitPort;
import dexter.banking.model.ApiConstants;
import dexter.banking.model.LimitManagementRequest;
import dexter.banking.model.LimitManagementResponse;
import dexter.banking.model.LimitManagementReversalRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(value = "limit-management-service")
public interface LimitAdapter extends LimitPort {

    @Override
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = ApiConstants.API_LIMIT_MANAGEMENT)
    LimitManagementResponse earmarkLimit(@RequestBody LimitManagementRequest limitManagementRequest);

    @Override
    @PutMapping(
            produces = MediaType.APPLICATION_JSON_VALUE,
            value = ApiConstants.API_LIMIT_MANAGEMENT + "/{limitEarmarkId}/cancelled")
    LimitManagementResponse reverseLimitEarmark(@PathVariable("limitEarmarkId") UUID limitEarmarkId,
                                                @RequestBody LimitManagementReversalRequest limitManagementReversalRequest);
}


