package dexter.banking.creditcard.services;

import dexter.banking.creditcard.model.CreditCardBankingInfo;
import dexter.banking.creditcard.model.CardType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import dexter.banking.model.CreditCardBankingReversalRequest;
import dexter.banking.model.CreditCardBankingRequest;
import dexter.banking.model.CreditCardBankingResponse;
import dexter.banking.model.CreditCardBankingStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreditCardBankingService {

    public static final int CURRENCY_EXPONENT = 2;

    private final List<CreditCardBankingInfo> creditCardBankingInfoList = List.of(

            CreditCardBankingInfo.builder()
                    .id(UUID.fromString("9f805fff-1912-48a6-ae06-5c4b906f4179"))
                    .cardNumber("123111")
                    .fullName("MS. JANE DOE")
                    .balanceDue(BigDecimal.valueOf(20670, CURRENCY_EXPONENT))
                    .limit(BigDecimal.valueOf(2130, CURRENCY_EXPONENT))
                    .cardType(CardType.DOMESTIC)
                    .available(false)
                    .build(),

            CreditCardBankingInfo.builder()
                    .id(UUID.fromString("9c9a85ca-4a78-4b4f-88a8-9f0690caf667"))
                    .cardNumber("123222")
                    .fullName("MR. JOHN DOE")
                    .balanceDue(BigDecimal.valueOf(5500, CURRENCY_EXPONENT))
                    .limit(BigDecimal.valueOf(530, CURRENCY_EXPONENT))
                    .cardType(CardType.INTERNATIONAL)
                    .available(true)
                    .build(),

            CreditCardBankingInfo.builder()
                    .id(UUID.fromString("9c9a85cb-4a78-4b4f-88a8-9f0690caf667"))
                    .cardNumber("123000")
                    .fullName("MR. SLEEPY JOE")
                    .balanceDue(BigDecimal.valueOf(5500, CURRENCY_EXPONENT))
                    .limit(BigDecimal.valueOf(530, CURRENCY_EXPONENT))
                    .cardType(CardType.INTERNATIONAL)
                    .available(true)
                    .build()
    );

    public List<CreditCardBankingInfo> getAll() {
        return creditCardBankingInfoList;
    }

    public CreditCardBankingResponse submitCreditCardBankingRequest(CreditCardBankingRequest creditCardBankingRequest) {

        Optional<CreditCardBankingInfo> creditCardBankingInfo = creditCardBankingInfoList.stream()
                .filter(ccInfo -> creditCardBankingRequest.getCardNumber().equals(ccInfo.getCardNumber()))
                .findFirst();

        //Simulate processing time
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return registerCreditCardBankingEvent(creditCardBankingRequest.getTransactionId(), creditCardBankingInfo);
    }

    private CreditCardBankingResponse registerCreditCardBankingEvent(UUID transactionId, Optional<CreditCardBankingInfo> ccInfo) {

        OffsetDateTime eventDateTime = OffsetDateTime.now();
        return CreditCardBankingResponse.builder()
                .id(UUID.randomUUID())
                .transactionId(transactionId)
                .createdDate(eventDateTime)
                .lastModifiedDate(eventDateTime)
                .creditCardBankingId(ccInfo.map(CreditCardBankingInfo::getId).orElse(null))
                .status(creditCardBankingInfoToStatus(ccInfo.orElse(null)))
                .build();
    }

    public CreditCardBankingInfo getCreditCardBankingInfo(UUID ccId) {
        return creditCardBankingInfoList.stream()
                .filter(cc -> ccId.equals(cc.getId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private CreditCardBankingStatus creditCardBankingInfoToStatus(CreditCardBankingInfo creditCardBankingInfo) {
        if (creditCardBankingInfo == null) {
            return CreditCardBankingStatus.NOT_FOUND;
        } else {
            return creditCardBankingInfo.isAvailable() ? CreditCardBankingStatus.SUCCESSFUL : CreditCardBankingStatus.FAILED;
        }
    }

    public CreditCardBankingResponse creditCardBankingReversal(CreditCardBankingReversalRequest creditCardBankingReversalRequest) {

        OffsetDateTime eventDateTime = OffsetDateTime.now();
        //Simulate processing time
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return CreditCardBankingResponse.builder()
                .id(creditCardBankingReversalRequest.getCreditReversalId())
                .transactionId(creditCardBankingReversalRequest.getTransactionId())
                .createdDate(eventDateTime)
                .lastModifiedDate(eventDateTime)
                .creditCardBankingId(UUID.randomUUID())
                .status(CreditCardBankingStatus.REVERSAL_SUCCESSFUL)
                .build();
    }
}
