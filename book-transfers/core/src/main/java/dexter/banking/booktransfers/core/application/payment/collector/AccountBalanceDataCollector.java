package dexter.banking.booktransfers.core.application.payment.collector;

import dexter.banking.booktransfers.core.application.payment.command.PaymentCommand;
import dexter.banking.booktransfers.core.domain.payment.valueobject.AccountBalanceFragment;
import dexter.banking.booktransfers.core.port.in.enrichment.DataCollector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Sample DataCollector for fetching account balance information.
 */
@Component("accountBalanceDataCollector") // Spring bean name for configuration
@Slf4j
class AccountBalanceDataCollector implements DataCollector<PaymentCommand, AccountBalanceFragment> {

    @Override
    public AccountBalanceFragment collect(PaymentCommand command) {
        log.info("Collecting account balance for command: {}", command.getAccountNumber());
        // Simulate fetching data from an external service
        return new AccountBalanceFragment(command.getAccountNumber(), new BigDecimal("12345.67"));
    }
}
