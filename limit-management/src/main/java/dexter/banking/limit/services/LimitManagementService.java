package dexter.banking.limit.services;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import dexter.banking.model.LimitManagementReversalRequest;
import dexter.banking.limit.model.LimitManagementInfo;
import dexter.banking.model.LimitManagementRequest;
import dexter.banking.model.LimitManagementResponse;
import dexter.banking.limit.model.CustomerType;
import dexter.banking.model.LimitEarmarkStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LimitManagementService {

    private final List<LimitManagementInfo> limitManagementInfos = List.of(

            LimitManagementInfo.builder()
                    .id(UUID.fromString("4e18afe0-3f1f-426b-a7c7-82e8be93036a"))
                    .limitType("Daily")
                    .accountType("Wallet")
                    .customerType(CustomerType.SME)
                    .available(true)
                    .build(),

            LimitManagementInfo.builder()
                    .id(UUID.fromString("3bde95ae-179e-4a8a-9351-fabd73611260"))
                    .limitType("Monthly")
                    .accountType("Savers Account")
                    .customerType(CustomerType.INDIVIDUAL)
                    .available(true)
                    .build(),

            LimitManagementInfo.builder()
                    .id(UUID.fromString("782a4090-ffc8-43f6-a93e-f042e056e80e"))
                    .limitType("Yearly")
                    .accountType("Card")
                    .customerType(CustomerType.CORPORATE)
                    .available(false)
                    .build()
    );

    public List<LimitManagementInfo> getAll() {
        return limitManagementInfos;
    }

    public LimitManagementResponse submitLimitManagementRequest(LimitManagementRequest limitManagementRequest) {

        Optional<LimitManagementInfo> limitManagementInfo = limitManagementInfos.stream()
                .filter(info -> limitManagementRequest.getLimitType().equals(info.getLimitType()))
                .findFirst();
        //Simulate processing time
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return registerLimitManagementEvent(limitManagementRequest.getTransactionId(), limitManagementInfo);
    }

    private LimitManagementResponse registerLimitManagementEvent(UUID transactionId, Optional<LimitManagementInfo> limitManagementInfo) {

        OffsetDateTime eventDateTime = OffsetDateTime.now();
        return LimitManagementResponse.builder()
                .id(UUID.randomUUID())
                .transactionId(transactionId)
                .createdDate(eventDateTime)
                .lastModifiedDate(eventDateTime)
                .limitId(limitManagementInfo.map(LimitManagementInfo::getId).orElse(null))
                .status(limitManagementInfoToStatus(limitManagementInfo.orElse(null)))
                .build();
    }

    public LimitManagementInfo getLimitManagementInfo(UUID limitManagementId) {
        return limitManagementInfos.stream()
                .filter(info -> limitManagementId.equals(info.getId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private LimitEarmarkStatus limitManagementInfoToStatus(LimitManagementInfo limitManagementInfo) {
        if (limitManagementInfo == null) {
            return LimitEarmarkStatus.NOT_FOUND;
        } else {
            return limitManagementInfo.isAvailable() ? LimitEarmarkStatus.SUCCESSFUL : LimitEarmarkStatus.FAILED;
        }
    }

    public LimitManagementResponse limitManagementReversalRequest(LimitManagementReversalRequest limitManagementReversalRequest) {
        // Reversal succeeds 80% of the time, simulating a real-world scenario
        LimitEarmarkStatus status = Math.random() < 0.8 ? LimitEarmarkStatus.REVERSAL_SUCCESSFUL : LimitEarmarkStatus.REVERSAL_FAILED;
        //Simulate processing time
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        OffsetDateTime eventDateTime = OffsetDateTime.now();
        return LimitManagementResponse.builder()
                .id(limitManagementReversalRequest.getLimitManagementId())
                .transactionId(limitManagementReversalRequest.getTransactionId())
                .createdDate(eventDateTime)
                .lastModifiedDate(eventDateTime)
                .limitId(UUID.randomUUID())
                .status(status)
                .build();
    }
}
