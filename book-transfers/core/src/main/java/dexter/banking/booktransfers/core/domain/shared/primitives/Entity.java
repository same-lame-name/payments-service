package dexter.banking.booktransfers.core.domain.shared.primitives;

import java.util.Objects;

/**
 * A base class for domain entities.
 * An Entity is defined not by its attributes, but by a thread of continuity and identity.
 * Equality is based solely on the entity's unique identifier.
 * @param <ID> The type of the entity's identifier.
 */
public abstract class Entity<ID> {

    protected final ID id;

    protected Entity(ID id) {
        this.id = Objects.requireNonNull(id, "Entity ID cannot be null");
    }

    public ID getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entity<?> entity = (Entity<?>) o;
        return id.equals(entity.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
