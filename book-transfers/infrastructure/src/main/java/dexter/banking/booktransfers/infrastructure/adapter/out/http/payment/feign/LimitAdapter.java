package dexter.banking.booktransfers.infrastructure.adapter.out.http.payment.feign;

import dexter.banking.booktransfers.core.application.payment.command.PaymentCommand;
import dexter.banking.booktransfers.core.domain.payment.Payment;
import dexter.banking.booktransfers.core.domain.payment.result.LimitEarmarkResult;
import dexter.banking.booktransfers.core.port.out.LimitPort;
import dexter.banking.model.ApiConstants;
import dexter.banking.model.LimitManagementRequest;
import dexter.banking.model.LimitManagementResponse;
import dexter.banking.model.LimitManagementReversalRequest;
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
class LimitAdapter implements LimitPort {

    private final RawLimitClient client;
    private final HttpAdapterMapper mapper;
    @Autowired
    public LimitAdapter(RawLimitClient client, HttpAdapterMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    public LimitEarmarkResult earmarkLimit(PaymentCommand command) {
        LimitManagementRequest request = mapper.toLimitManagementRequest(command);
        LimitManagementResponse responseDto = client.earmarkLimit(request);
        return mapper.toDomain(responseDto);
    }

    @Override
    public LimitEarmarkResult reverseLimitEarmark(Payment payment) {
        LimitManagementReversalRequest request = mapper.toLimitEarmarkReversalRequest(payment);
        LimitManagementResponse responseDto = client.reverseLimitEarmark(payment.getLimitEarmarkResult().limitId(), request);
        return mapper.toReversalDomain(responseDto);
    }

    @FeignClient(value = "limit-management-service")
    interface RawLimitClient {

        @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = ApiConstants.API_LIMIT_MANAGEMENT)
        LimitManagementResponse earmarkLimit(@RequestBody LimitManagementRequest limitManagementRequest);
        @PutMapping(
                produces = MediaType.APPLICATION_JSON_VALUE,
                value = ApiConstants.API_LIMIT_MANAGEMENT + "/{limitEarmarkId}/cancelled")
        LimitManagementResponse reverseLimitEarmark(@PathVariable("limitEarmarkId") UUID limitEarmarkId,
                                                      @RequestBody LimitManagementReversalRequest limitManagementReversalRequest);
    }
}
