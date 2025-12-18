package dexter.banking.limit.pipeline.payee.middleware;

import dexter.banking.limit.config.model.RulesConfig;
import dexter.banking.limit.pipeline.core.PipelineMiddleware;
import dexter.banking.limit.web.dto.UpdatePayeePatch;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.util.Set;

@Component
@Slf4j
public class StrictMergePatchValidator implements PipelineMiddleware<UpdatePayeePatch> {

    @Override
    public <R> R process(UpdatePayeePatch request, Next<R> next) {
        RulesConfig rules = request.getRulesConfig();
        if (rules == null) throw new IllegalStateException("Rules config missing");

        Set<String> allowed = rules.editableFields();

        try {
            validate(request, allowed, null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Reflection failed", e);
        }
        return next.invoke();
    }

    private void validate(Object obj, Set<String> allowed, String parent) throws IllegalAccessException {
        ReflectionUtils.doWithFields(obj.getClass(), field -> {
            field.setAccessible(true);
            if (JsonNullable.class.isAssignableFrom(field.getType())) {
                JsonNullable<?> wrapper = (JsonNullable<?>) field.get(obj);

                if (wrapper != null && wrapper.isPresent()) {
                    String path = parent == null ? field.getName() : parent + "." + field.getName();
                    Object val = wrapper.get();

                    if (shouldRecurse(val)) {
                        validate(val, allowed, path);
                    } else {
                        if (!allowed.contains(path)) {
                             throw new IllegalArgumentException("Field '" + path + "' is not editable.");
                        }
                    }
                }
            }
        });
    }

    private boolean shouldRecurse(Object val) {
        if (val == null) return false;
        Class<?> c = val.getClass();
        return !c.getPackageName().startsWith("java.")
            && !Enum.class.isAssignableFrom(c)
            && c.getPackageName().startsWith("dexter.banking"); // Only recurse our DTOs
    }

    @Override
    public int getOrder() { return 50; }
}
