package dexter.banking.booktransfers.infrastructure.provider;

import dexter.banking.booktransfers.core.domain.shared.blueprint.BeanReference;
import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyBlueprint;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

import java.lang.reflect.*;
import java.util.List;
import java.util.stream.Collectors;

public final class BlueprintProxyFactory {

    private BlueprintProxyFactory() {
    }

    @SuppressWarnings("unchecked")
    public static <T extends JourneyBlueprint> T createProxy(
            Class<T> blueprintInterface,
            Object properties, // Backed by a type-safe @ConfigurationProperties object
            ApplicationContext ctx
    ) {
        return (T) Proxy.newProxyInstance(
                blueprintInterface.getClassLoader(),
                new Class<?>[]{blueprintInterface},
                new BlueprintInvocationHandler(properties, ctx)
        );
    }

    private static class BlueprintInvocationHandler implements InvocationHandler {
        private final Object properties;
        private final ApplicationContext ctx;

        public BlueprintInvocationHandler(Object properties, ApplicationContext ctx) {
            this.properties = properties;
            this.ctx = ctx;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Class<?> returnType = method.getReturnType();
            Method propertiesGetter = properties.getClass().getMethod(method.getName());
            Object configuredValue = propertiesGetter.invoke(properties);

            // 1. Is the return type a nested blueprint?
            if (JourneyBlueprint.class.isAssignableFrom(returnType)) {
                if (configuredValue == null) {
                    throw new IllegalStateException(String.format(
                            "Configuration missing for mandatory nested blueprint '%s'", method.getName()
                    ));
                }
                return createProxy((Class<? extends JourneyBlueprint>) returnType, configuredValue, ctx);
            }
            // 2. Is it a reference to a Spring bean (or a List of beans)?
            else if (method.isAnnotationPresent(BeanReference.class)) {
                if (configuredValue == null) {
                    throw new IllegalStateException("Configuration for @BeanReference key '" + method.getName() + "' is missing.");
                }
                // Handle List<Bean>
                if (returnType.equals(List.class)) {
                    Type genericReturnType = method.getGenericReturnType();
                    if (!(genericReturnType instanceof ParameterizedType)) {
                        throw new IllegalStateException("@BeanReference on a List must have a generic type (e.g., List<MyBean>).");
                    }
                    Type beanType = ((ParameterizedType) genericReturnType).getActualTypeArguments()[0];
                    Class<?> beanClass = (Class<?>) beanType;

                    List<String> beanNames = (List<String>) configuredValue;
                    return beanNames.stream()
                            .map(name -> ctx.getBean(name, beanClass))
                            .collect(Collectors.toList());
                }
                // Handle single bean
                else {
                    String beanName = (String) configuredValue;
                    if (!StringUtils.hasText(beanName)) {
                        throw new IllegalStateException("Configuration for @BeanReference key '" + method.getName() + "' is empty.");
                    }
                    return ctx.getBean(beanName, returnType);
                }
            }
            // 3. If none of the above, it is a simple literal property.
            // Any other scenario, as in a
            else {
                if (configuredValue == null) {
                    throw new IllegalStateException("Configuration for mandatory blueprint property '" + method.getName() + "' is missing.");
                }
                return configuredValue;
            }
        }
    }
}
