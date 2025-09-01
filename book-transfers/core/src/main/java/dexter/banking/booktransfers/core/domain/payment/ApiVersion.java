package dexter.banking.booktransfers.core.domain.payment;

/**
 * A type-safe discriminator for identifying the requested API version of a command.
 */
public enum ApiVersion {
    V1, // Represents the direct, procedural flow
    V2  // Represents the orchestrated (state machine) flow
}
