package dexter.banking.booktransfers.infrastructure;

/**
 * A marker interface for all public infrastructure adapter @Configuration classes.
 *
 * This interface is used by a custom ComponentScan filter in the main application
 * to discover and activate infrastructure "plugins" in a controlled and explicit way.
 * An adapter is only active if its public facade implements this interface.
 */
public interface FacadeConfiguration {
}
