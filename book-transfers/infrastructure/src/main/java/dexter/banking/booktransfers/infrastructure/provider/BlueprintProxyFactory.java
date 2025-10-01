package dexter.banking.booktransfers.infrastructure.provider;

import dexter.banking.booktransfers.core.domain.shared.blueprint.ExtractBean;
import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyBlueprint;
import dexter.banking.booktransfers.core.domain.shared.blueprint.VerifyBean;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class BlueprintProxyFactory {

    private BlueprintProxyFactory() {
    }

    @SuppressWarnings("unchecked")
    public static <T extends JourneyBlueprint> T createProxy(
            Class<T> blueprintInterface,
            Object properties,
            ApplicationContext ctx
    ) {
        return (T) Proxy.newProxyInstance(
                blueprintInterface.getClassLoader(),
                new Class<?>[]{blueprintInterface},
                new BlueprintInvocationHandler(blueprintInterface, properties, ctx)
        );
    }

    private static class BlueprintInvocationHandler implements InvocationHandler {
        // This cache now holds a mix of EAGERLY created proxies and LAZILY prepared suppliers.
        private final Map<Method, Object> methodCache = new HashMap<>();

        public BlueprintInvocationHandler(Class<?> blueprintInterface, Object properties, ApplicationContext ctx) {
            // The constructor now recursively builds the entire graph.
            buildCache(blueprintInterface, properties, ctx);
        }

        private void buildCache(Class<?> blueprintInterface, Object properties, ApplicationContext ctx) {
            for (Method method : blueprintInterface.getMethods()) {
                if (method.getDeclaringClass().equals(Object.class) || methodCache.containsKey(method)) {
                    continue;
                }

                try {
                    // If it's a nested blueprint (intermediate node), EAGERLY create the proxy.
                    if (JourneyBlueprint.class.isAssignableFrom(method.getReturnType())) {
                        Method propertiesGetter = properties.getClass().getMethod(method.getName());
                        Object nestedProperties = propertiesGetter.invoke(properties);
                        if (nestedProperties == null)
                            throw new IllegalStateException("Missing config for nested blueprint '" + method.getName() + "'");

                        // The recursion happens here, during construction. The result is a real proxy.
                        Object nestedProxy = createProxy((Class<? extends JourneyBlueprint>) method.getReturnType(), nestedProperties, ctx);
                        methodCache.put(method, nestedProxy);
                    }
                    // If it's a leaf node (bean or literal), create a LAZY recipe.
                    else {
                        Supplier<Object> recipe = createRecipeForLeaf(method, properties, ctx);
                        methodCache.put(method, recipe);
                    }
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to build blueprint cache for method '" + method.getName() + "'", e);
                }
            }
        }

        private Supplier<Object> createRecipeForLeaf(Method method, Object properties, ApplicationContext ctx) throws NoSuchMethodException {
            Method propertiesGetter = properties.getClass().getMethod(method.getName());

            // === CASE 1: @ExtractBean ===
            if (method.isAnnotationPresent(ExtractBean.class)) {
                return () -> {
                    try {
                        Object configuredValue = propertiesGetter.invoke(properties);
                        if (configuredValue == null)
                            throw new IllegalStateException("Missing config for @ExtractBean '" + method.getName() + "'");

                        if (method.getReturnType().equals(List.class)) {
                            Type beanType = ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
                            List<String> beanNames = (List<String>) configuredValue;
                            return beanNames.stream()
                                    .map(name -> ctx.getBean(name, (Class<?>) beanType))
                                    .collect(Collectors.toList());
                        } else {
                            String beanName = (String) configuredValue;
                            if (!StringUtils.hasText(beanName))
                                throw new IllegalStateException("Empty config for @ExtractBean '" + method.getName() + "'");
                            return ctx.getBean(beanName, method.getReturnType());
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                };
            }
            // === CASE 2: @VerifyBean ===
            else if (method.isAnnotationPresent(VerifyBean.class)) {
                return () -> {
                    try {
                        Object configuredValue = propertiesGetter.invoke(properties);
                        if (configuredValue == null)
                            throw new IllegalStateException("Missing config for @VerifyBean '" + method.getName() + "'");

                        if (configuredValue instanceof List) {
                            List<String> beanNames = (List<String>) configuredValue;
                            for (String beanName : beanNames) {
                                if (!ctx.containsBean(beanName))
                                    throw new IllegalStateException("Verified bean not found: " + beanName);
                            }
                        } else {
                            String beanName = (String) configuredValue;
                            if (!ctx.containsBean(beanName))
                                throw new IllegalStateException("Verified bean not found: ".concat(beanName));
                        }
                        // IMPORTANT: Return the original configured value, not the bean.
                        return configuredValue;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                };
            }
            // === CASE 3: Simple Literal ===
            else {
                return () -> {
                    try {
                        Object configuredValue = propertiesGetter.invoke(properties);
                        if (configuredValue == null)
                            throw new IllegalStateException("Missing config for mandatory property '" + method.getName() + "'");
                        return configuredValue;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                };
            }
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            Object cachedValue = methodCache.get(method);

            // If the cached value is a recipe, execute it.
            if (cachedValue instanceof Supplier) {
                return ((Supplier<?>) cachedValue).get();
            }
            // Otherwise, it's a pre-built nested proxy. Return it directly.
            else {
                return cachedValue;
            }
        }
    }
}
