package dexter.banking.booktransfers.infrastructure.provider;

import dexter.banking.booktransfers.core.domain.shared.blueprint.BeanReference;
import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyBlueprint;
import org.springframework.context.ApplicationContext;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

final class BlueprintProxyFactory {

    private BlueprintProxyFactory() {}

    @SuppressWarnings("unchecked")
    public static <T extends JourneyBlueprint> T createProxy(
        Class<T> blueprintInterface,
        Map<String, Object> config,
        ApplicationContext ctx
    ) {
        return (T) Proxy.newProxyInstance(
            blueprintInterface.getClassLoader(),
            new Class<?>[]{blueprintInterface},
            new BlueprintInvocationHandler(config, ctx)
        );
    }

    private static class BlueprintInvocationHandler implements InvocationHandler {
        private final Map<String, Object> config;
        private final ApplicationContext ctx;

        public BlueprintInvocationHandler(Map<String, Object> config, ApplicationContext ctx) {
            this.config = config;
            this.ctx = ctx;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String key = method.getName();
            Object value = config.get(key);
            Class<?> returnType = method.getReturnType();

            if (value == null) {
                throw new IllegalStateException(String.format(
                    "Configuration key '%s' not found in journey config for blueprint '%s'",
                    key, method.getDeclaringClass().getSimpleName()
                ));
            }

            if (JourneyBlueprint.class.isAssignableFrom(returnType)) {
                // This logic for nested blueprints remains correct.
                return createProxy(
                    (Class<? extends JourneyBlueprint>) returnType,
                    (Map<String, Object>) value,
                    ctx
                );
            } else if (method.isAnnotationPresent(BeanReference.class)) {
                if (!(value instanceof String)) {
                    throw new IllegalStateException(String.format(
                        "Configuration error: Key '%s' is marked as @BeanReference but value is not a string.", key
                    ));
                }
                String beanName = (String) value;
                if (!ctx.containsBean(beanName)) {
                     throw new IllegalStateException(String.format(
                        "Configuration error: Bean with name '%s' referenced by key '%s' does not exist.",
                        beanName, key
                     ));
                }
                return ctx.getBean(beanName, returnType);
            } else {
                // If no annotation, return the value as a literal.
                return value;
            }
        }
    }
}
