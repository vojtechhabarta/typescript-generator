
package cz.habarta.typescript.generator;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomMappingTypeProcessor implements TypeProcessor {

    private final Map<String, String> customMappings = new HashMap<>();

    public CustomMappingTypeProcessor(List<String> customMappings) {
        if (customMappings != null) {
            for (String customMapping : customMappings) {
                this.customMappings.putAll(convertToMap(customMapping));
            }
        }
    }

    @Override
    public Result processType(Type javaType, Context context) {
        final Class<?> rawClass = Utils.getRawClassOrNull(javaType);
        if (rawClass != null) {
            String type = customMappings.get(rawClass.getName());
            if (type != null) {
                return new Result(new TsType.BasicType(type));
            }
        }

        return null;
    }

    private Map<String, String> convertToMap(String mappingString) {

        Map<String, String> customMapping = new HashMap<>();
        String[] values = mappingString.split(":");
        customMapping.put(values[0], values[1]);

        return customMapping;
    }

}
