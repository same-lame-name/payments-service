package dexter.banking.limit.pipeline.payee.middleware;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import dexter.banking.limit.config.model.RulesConfig;
import dexter.banking.limit.pipeline.core.PipelineMiddleware;
import dexter.banking.limit.util.JsonNullableOnlyModule;
import dexter.banking.limit.web.dto.UpdatePayeePatch;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.jackson.nullable.JsonNullable;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class StrictMergePatchValidator implements PipelineMiddleware<UpdatePayeePatch> {

    // 1. ISOLATED MAPPER: Standard behavior, no side effects
    private static final ObjectMapper VALIDATION_MAPPER;

    static {
        VALIDATION_MAPPER = new ObjectMapper();
        VALIDATION_MAPPER.registerModule(new JsonNullableModule());
        VALIDATION_MAPPER.registerModule(new JsonNullableOnlyModule());

        // CRITICAL: We must see "null" nodes to detect Delete instructions
        VALIDATION_MAPPER.setSerializationInclusion(JsonInclude.Include.ALWAYS);
    }

    @Override
    public <R> R process(UpdatePayeePatch request, Next<R> next) {
        RulesConfig rules = request.getRulesConfig();
        if (rules == null) throw new IllegalStateException("Rules config missing");

        Set<String> allowed = rules.editableFields();

        // 2. CONVERT: DTO -> JsonNode (The "Truth")
        JsonNode tree = VALIDATION_MAPPER.valueToTree(request);

        // 3. WALK: Recursive check
        validateNode(tree, "", allowed);

        return next.invoke();
    }

    private void validateNode(JsonNode node, String prefix, Set<String> allowed) {
        if (node.isObject()) {
            // iterator() gives us the keys (Field Names) which we need for validation
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String fieldName = entry.getKey();
                JsonNode child = entry.getValue();

                String path = prefix.isEmpty() ? fieldName : prefix + "." + fieldName;

                if (child.isObject()) {
                    // RECURSE: It is a container (e.g. "address"), look inside.
                    validateNode(child, path, allowed);
                } else {
                    // LEAF: It is an Instruction (Update OR Delete/Null).
                    // Validate the path against the Allowlist.
                    checkAllowed(path, allowed);
                }

            }
        }
    }

    private void checkAllowed(String path, Set<String> allowed) {
        if (!allowed.contains(path)) {
            log.warn("Attempted update on locked field: {}", path);
            throw new IllegalArgumentException("Field '" + path + "' is not editable.");
        }
    }

    @Override
    public int getOrder() { return 50; }
}
