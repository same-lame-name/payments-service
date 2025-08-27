package dexter.banking.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepositBankingReversalRequest {

    @NotNull
    private UUID transactionId;

    @NotNull
    private UUID reservationId;
}
