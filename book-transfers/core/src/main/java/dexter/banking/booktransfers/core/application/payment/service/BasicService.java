package dexter.banking.booktransfers.core.application.payment.service;

import dexter.banking.booktransfers.core.domain.shared.context.BeginJourney;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BasicService {

    @BeginJourney("'BASIC_SERVICE'")
    @Transactional
    public void submit() {

    }
}
