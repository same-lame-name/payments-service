package dexter.banking.booktransfers.core.application.payment.collector;

import dexter.banking.booktransfers.core.application.payment.command.PaymentCommand;
import dexter.banking.booktransfers.core.domain.payment.valueobject.CustomerProfileFragment;
import dexter.banking.booktransfers.core.port.in.enrichment.DataCollector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Sample DataCollector for fetching customer profile information.
 */
@Component("customerProfileDataCollector") // Spring bean name for configuration
@Slf4j
public class CustomerProfileDataCollector implements DataCollector<PaymentCommand, CustomerProfileFragment> {

    @Override
    public CustomerProfileFragment collect(PaymentCommand command) {
        log.info("Collecting customer profile for command: {}", command.getTransactionReference());
        // Simulate fetching data from an external service
        return new CustomerProfileFragment(command.getAccountNumber(), "John Doe", "john.doe@example.com");
    }
}
