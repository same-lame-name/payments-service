package dexter.banking.booktransfers.core.domain.shared.primitives;

/**
 * A marker interface for Value Objects.
 * A Value Object is an object that represents a descriptive aspect of the domain
 * with no conceptual identity. They are immutable and their equality is based
 * on the structural equality of their fields. Java's `record` is the ideal
 * implementation choice for this contract.
 */
public interface ValueObject {
}
