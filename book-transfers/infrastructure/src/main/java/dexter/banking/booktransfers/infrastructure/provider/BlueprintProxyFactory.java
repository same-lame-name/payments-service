package dexter.banking.booktransfers.infrastructure.provider;

import dexter.banking.booktransfers.core.domain.shared.blueprint.BeanReference;
import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyBlueprint;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public final class BlueprintProxyFactory {

    private BlueprintProxyFactory() {}

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

            // 1. If the return type is a nested blueprint, create a recursive proxy.
            if (JourneyBlueprint.class.isAssignableFrom(returnType)) {
                Method propertiesGetter = properties.getClass().getMethod(method.getName());
                Object nestedProperties = propertiesGetter.invoke(properties);
                if (nestedProperties == null) {
                    throw new IllegalStateException(String.format(
                        "Configuration missing for nested blueprint '%s' in journey", method.getName()
                    ));
                }
                return createProxy((Class<? extends JourneyBlueprint>) returnType, nestedProperties, ctx);
            }
            // 2. If the method is a bean reference, resolve the bean from the context.
            else if (method.isAnnotationPresent(BeanReference.class)) {
                Method propertiesGetter = properties.getClass().getMethod(method.getName());
                String beanName = (String) propertiesGetter.invoke(properties);
                if (!StringUtils.hasText(beanName)) {
                    throw new IllegalStateException(String.format(
                        "Configuration key '%s' for @BeanReference is missing or empty.", method.getName()
                    ));
                }
                return ctx.getBean(beanName, returnType);
            }
            // 3. Otherwise, it's a simple property. Delegate the call to the backing properties object.
            else {
                Method propertiesGetter = properties.getClass().getMethod(method.getName());
                return propertiesGetter.invoke(properties);
            }
        }
    }
}
