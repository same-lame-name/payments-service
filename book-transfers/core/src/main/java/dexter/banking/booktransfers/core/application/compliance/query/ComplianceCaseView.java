package dexter.banking.booktransfers.core.application.compliance.query;

import dexter.banking.booktransfers.core.domain.compliance.ComplianceStatus;

import java.util.UUID;

/**
 * The dedicated Read Model for compliance case queries.
 * This is an immutable, flat DTO containing only the data needed by query clients.
 */
public record ComplianceCaseView(
    UUID caseId,
    UUID paymentId,
    ComplianceStatus status
) {}
