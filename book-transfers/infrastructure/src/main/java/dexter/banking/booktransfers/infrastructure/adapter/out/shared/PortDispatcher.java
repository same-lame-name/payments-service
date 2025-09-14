package dexter.banking.booktransfers.infrastructure.adapter.out.shared;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A generic, reusable dispatcher interface that provides default implementations
 * for the common logic of routing to port adapters.
 *
 * @param <P> The type of the Port interface this dispatcher serves (e.g., DepositPort).
 */
public interface PortDispatcher<P> {

    /**
     * Builds a map of bean names to adapter instances.
     * This is a default method providing a reusable implementation.
     *
     * @param allAdapters The list of all beans implementing the port interface.
     * @param context     The Spring ApplicationContext to look up bean names.
     * @return A map of bean name to adapter instance.
     */
    default Map<String, P> buildAdapterMap(List<P> allAdapters, ApplicationContext context) {
        return allAdapters.stream()
                // The dispatcher itself must be filtered out to prevent recursion.
                // We assume the concrete dispatcher class will implement this interface.
                .filter(adapter -> !(this.getClass().isInstance(adapter)))
                .collect(Collectors.toMap(
                        adapter -> getBeanName(context, adapter),
                        Function.identity()
                ));
    }

    private String getBeanName(ApplicationContext context, Object bean) {
        try {
            String[] beanNames = context.getBeanNamesForType(bean.getClass());
            if (beanNames.length == 1) {
                return beanNames[0];
            }
            if (beanNames.length > 1) {
                throw new IllegalStateException("Found multiple bean names for adapter class: " + bean.getClass().getName());
            }
        } catch (NoSuchBeanDefinitionException e) {
            // This should not happen if the bean was injected into the list.
        }
        throw new IllegalStateException("Could not determine bean name for adapter: " + bean.getClass().getName());
    }
}