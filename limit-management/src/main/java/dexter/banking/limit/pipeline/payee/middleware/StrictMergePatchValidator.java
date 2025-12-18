package dexter.banking.limit.pipeline.payee.middleware;

import dexter.banking.limit.config.model.RulesConfig;
import dexter.banking.limit.pipeline.core.PipelineMiddleware;
import dexter.banking.limit.web.dto.UpdatePayeePatch;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class StrictMergePatchValidator implements PipelineMiddleware<UpdatePayeePatch> {

    private static final List<Field> PATCH_FIELDS;

    static {
        List<Field> f = new ArrayList<>();
        ReflectionUtils.doWithFields(UpdatePayeePatch.class, field -> {
            if (JsonNullable.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                f.add(field);
            }
        });
        PATCH_FIELDS = Collections.unmodifiableList(f);
    }

    @Override
    public <R> R process(UpdatePayeePatch request, Next<R> next) {

        RulesConfig rules = request.getRulesConfig();
        if (rules == null) {
            throw new IllegalStateException("Rules config not loaded for request");
        }

        Set<String> allowedFields = rules.editableFields();

        for (Field field : PATCH_FIELDS) {
            try {
                JsonNullable<?> wrapper = (JsonNullable<?>) field.get(request);
                if (wrapper != null && wrapper.isPresent()) {
                    if (!allowedFields.contains(field.getName())) {
                        log.warn("Client attempted to update locked field: {}", field.getName());
                        throw new IllegalArgumentException("Field '" + field.getName() + "' is not editable in this context.");
                    }
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Failed to access field via reflection", e);
            }
        }

        return next.invoke();
    }

    @Override
    public int getOrder() {
        return 50;
    }
}