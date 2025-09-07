package dexter.banking.booktransfers.core.domain.compliance;

import dexter.banking.booktransfers.core.domain.compliance.event.ComplianceCaseApproved;
import dexter.banking.booktransfers.core.domain.compliance.event.ComplianceCaseRejected;
import dexter.banking.booktransfers.core.domain.shared.primitives.AggregateRoot;
import lombok.Getter;

import java.util.UUID;

@Getter
public class ComplianceCase extends AggregateRoot<UUID> {
    private final UUID paymentId;
    private ComplianceStatus status;
    private String reason;

    private ComplianceCase(UUID id, UUID paymentId, ComplianceStatus status, String reason) {
        super(id);
        this.paymentId = paymentId;
        this.status = status;
        this.reason = reason;
    }

    public static ComplianceCase create(UUID paymentId) {
        return new ComplianceCase(UUID.randomUUID(), paymentId, ComplianceStatus.PENDING_VERIFICATION, null);
    }

    public static ComplianceCase rehydrate(UUID id, UUID paymentId, ComplianceStatus status, String reason) {
        return new ComplianceCase(id, paymentId, status, reason);
    }

    public void approve() {
        if (this.status != ComplianceStatus.PENDING_VERIFICATION) {
            throw new IllegalStateException("Cannot approve a compliance case that is not in PENDING_VERIFICATION state. Current state: " + this.status);
        }
        this.status = ComplianceStatus.APPROVED;
        this.registerEvent(new ComplianceCaseApproved(this.id, this.paymentId));
    }

    public void reject(String reason) {
        if (this.status != ComplianceStatus.PENDING_VERIFICATION) {
            throw new IllegalStateException("Cannot reject a compliance case that is not in PENDING_VERIFICATION state. Current state: " + this.status);
        }
        this.status = ComplianceStatus.REJECTED;
        this.reason = reason;
        this.registerEvent(new ComplianceCaseRejected(this.id, this.paymentId, reason));
    }
}
