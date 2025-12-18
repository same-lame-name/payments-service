package dexter.banking.limit.config.idempotency;

import dexter.banking.limit.web.dto.PayeeDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TypeRegistry {

    private final Map<String, Class<?>> registry = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // Register all your safe DTOs here
        register("PAYEE_DTO", PayeeDto.class);
    }

    public void register(String alias, Class<?> clazz) {
        registry.put(alias, clazz);
    }

    public Class<?> getClass(String alias) {
        Class<?> clazz = registry.get(alias);
        if (clazz == null) {
            throw new IllegalArgumentException("No class registered for alias: " + alias);
        }
        return clazz;
    }

    public String getAlias(Class<?> clazz) {
        return registry.entrySet().stream()
                .filter(entry -> entry.getValue().equals(clazz))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No alias registered for class: " + clazz.getName()));
    }
}