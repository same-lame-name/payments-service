package dexter.banking.limit.util;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import org.openapitools.jackson.nullable.JsonNullable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A Jackson Module that strips away all properties NOT of type JsonNullable.
 * Used for Strict Patch Validation to isolate instructions from metadata.
 */
public class JsonNullableOnlyModule extends SimpleModule {

    @Override
    public void setupModule(SetupContext context) {
        super.setupModule(context);

        context.addBeanSerializerModifier(new BeanSerializerModifier() {
            @Override
            public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                                                             BeanDescription beanDesc,
                                                             List<BeanPropertyWriter> beanProperties) {
                return beanProperties.stream()
                    .filter(writer -> {
                        Class<?> rawClass = writer.getType().getRawClass();
                        return JsonNullable.class.isAssignableFrom(rawClass);
                    })
                    .collect(Collectors.toList());
            }
        });
    }
}
