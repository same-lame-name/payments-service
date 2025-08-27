package dexter.banking.deposit.services;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import dexter.banking.model.DepositBankingReversalRequest;
import dexter.banking.deposit.model.DepositBankingInfo;
import dexter.banking.model.DepositBankingRequest;
import dexter.banking.model.DepositBankingResponse;
import dexter.banking.model.DepositBankingStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DepositBankingService {

    public static final int CURRENCY_EXPONENT = 2;

    private final List<DepositBankingInfo> depositBankingInfoList = List.of(

            DepositBankingInfo.builder()
                    .id(UUID.fromString("193f7706-9093-449f-95e0-7f720b98e7f1"))
                    .accountName("Current Account")
                    .accountNumber("1312133")
                    .balance(BigDecimal.valueOf(10000, CURRENCY_EXPONENT))
                    .available(false)
                    .build(),

            DepositBankingInfo.builder()
                    .id(UUID.fromString("e856051a-0f3e-4b88-b5e4-6c65c6276d31"))
                    .accountName("Savings Account")
                    .accountNumber("1312134")
                    .balance(BigDecimal.valueOf(80000, CURRENCY_EXPONENT))
                    .available(true)
                    .build()
    );

    public List<DepositBankingInfo> getAll() {
        return depositBankingInfoList;
    }

    public DepositBankingResponse submitDepositBankingRequest(DepositBankingRequest depositBankingRequest) {

        Optional<DepositBankingInfo> depositBankingInfo = depositBankingInfoList.stream()
                .filter(info -> depositBankingRequest.getAccountNumber().equals(info.getAccountNumber()))
                .findFirst();
        //Simulate processing time
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return registerDepositBankingEvent(depositBankingRequest.getTransactionId(), depositBankingInfo);
    }

    private DepositBankingStatus depositBankingInfoToStatus(DepositBankingInfo depositBankingInfo) {
        if (depositBankingInfo == null) {
            return DepositBankingStatus.NOT_FOUND;
        } else {
            return depositBankingInfo.isAvailable() ? DepositBankingStatus.SUCCESSFUL : DepositBankingStatus.FAILED;
        }
    }

    private DepositBankingResponse registerDepositBankingEvent(UUID transactionId, Optional<DepositBankingInfo> depositBankingInfo) {

        OffsetDateTime eventDateTime = OffsetDateTime.now();
        return DepositBankingResponse.builder()
                .id(UUID.randomUUID())
                .transactionId(transactionId)
                .createdDate(eventDateTime)
                .lastModifiedDate(eventDateTime)
                .depositId(depositBankingInfo.map(DepositBankingInfo::getId).orElse(null))
                .status(depositBankingInfoToStatus(depositBankingInfo.orElse(null)))
                .build();
    }

    public DepositBankingInfo getDepositBankingInfo(UUID depositBankingId) {
        return depositBankingInfoList.stream()
                .filter(info -> depositBankingId.equals(info.getId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public DepositBankingResponse submitDepositBankingReversal(DepositBankingReversalRequest depositBankingReversalRequest) {
        //Reversal will succeed 80% of the time, simulating a real-world scenario
        DepositBankingStatus reversalResult = Math.random() < 0.8 ? DepositBankingStatus.REVERSAL_SUCCESSFUL : DepositBankingStatus.REVERSAL_FAILED;
        //Simulate processing time
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        OffsetDateTime eventDateTime = OffsetDateTime.now();
        return DepositBankingResponse.builder()
                .id(depositBankingReversalRequest.getReservationId())
                .transactionId(depositBankingReversalRequest.getTransactionId())
                .createdDate(eventDateTime)
                .lastModifiedDate(eventDateTime)
                .depositId(UUID.randomUUID())
                .status(reversalResult)
                .build();
    }
}
