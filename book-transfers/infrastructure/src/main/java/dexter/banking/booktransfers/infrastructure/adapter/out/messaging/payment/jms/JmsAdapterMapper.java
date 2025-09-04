package dexter.banking.booktransfers.infrastructure.adapter.out.messaging.payment.jms;

import dexter.banking.booktransfers.core.port.out.CreditCardPort;
import dexter.banking.booktransfers.core.port.out.DepositPort;
import dexter.banking.booktransfers.core.port.out.LimitPort;
import dexter.banking.model.CreditCardBankingRequest;
import dexter.banking.model.DepositBankingRequest;
import dexter.banking.model.DepositBankingReversalRequest;
import dexter.banking.model.LimitManagementRequest;
import dexter.banking.model.LimitManagementReversalRequest;
import org.mapstruct.Mapper;

/**
 * A dedicated Anti-Corruption Layer (ACL) mapper for the outbound JMS adapter.
 * It translates the core's lean Parameter Objects into external request DTOs for messaging.
 */
@Mapper(componentModel = "spring")
interface JmsAdapterMapper {
    LimitManagementRequest toLimitManagementRequest(LimitPort.EarmarkLimitRequest request);

    DepositBankingRequest toDepositBankingRequest(DepositPort.SubmitDepositRequest request);

    CreditCardBankingRequest toCreditCardBankingRequest(CreditCardPort.SubmitCreditCardPaymentRequest request);

    LimitManagementReversalRequest toLimitEarmarkReversalRequest(LimitPort.ReverseLimitEarmarkRequest request);

    DepositBankingReversalRequest toDepositReversalRequest(DepositPort.SubmitDepositReversalRequest request);
}
