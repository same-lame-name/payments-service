package dexter.banking.booktransfers.infrastructure.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
class RejectComplianceRequest {
    @NotBlank(message = "A reason must be provided for rejection")
    private String reason;
}
