
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;


public class Jackson2ConfigurationResolved {

    public JsonAutoDetect.Visibility fieldVisibility;
    public JsonAutoDetect.Visibility getterVisibility;
    public JsonAutoDetect.Visibility isGetterVisibility;
    public JsonAutoDetect.Visibility setterVisibility;
    public JsonAutoDetect.Visibility creatorVisibility;
    public Map<Class<?>, JsonFormat.Shape> shapeConfigOverrides;
    public boolean enumsUsingToString;
    public boolean disableObjectIdentityFeature;
    @SuppressWarnings("rawtypes")
    public Map<Class<? extends JsonSerializer>, String> serializerTypeMappings;
    @SuppressWarnings("rawtypes")
    public Map<Class<? extends JsonDeserializer>, String> deserializerTypeMappings;

    public static Jackson2ConfigurationResolved from(Jackson2Configuration configuration, ClassLoader classLoader) {
        final Jackson2ConfigurationResolved resolved = new Jackson2ConfigurationResolved();
        resolved.fieldVisibility = configuration.fieldVisibility;
        resolved.getterVisibility = configuration.getterVisibility;
        resolved.isGetterVisibility = configuration.isGetterVisibility;
        resolved.setterVisibility = configuration.setterVisibility;
        resolved.creatorVisibility = configuration.creatorVisibility;
        resolved.fieldVisibility = configuration.fieldVisibility;
        resolved.shapeConfigOverrides = resolveClassMappings(
            configuration.shapeConfigOverrides, classLoader, Object.class, JsonFormat.Shape::valueOf);
        resolved.enumsUsingToString = configuration.enumsUsingToString;
        resolved.disableObjectIdentityFeature = configuration.disableObjectIdentityFeature;
        resolved.deserializerTypeMappings = resolveClassMappings(
            configuration.deserializerTypeMappings, classLoader, JsonDeserializer.class, Function.identity());
        resolved.serializerTypeMappings = resolveClassMappings(
            configuration.serializerTypeMappings, classLoader, JsonSerializer.class, Function.identity());
        return resolved;
    }

    private static <C, V> Map<Class<? extends C>, V> resolveClassMappings(List<String> mappings, ClassLoader classLoader,
            Class<? extends C> key, Function<String, V> valueConvertor) {
        if (mappings == null) {
            return null;
        }
        final Map<Class<? extends C>, V> resolvedMappings = new LinkedHashMap<>();
        final Map<String, String> mappingsMap = Settings.convertToMap(mappings);
        for (Map.Entry<String, String> entry : mappingsMap.entrySet()) {
            final Class<? extends C> cls = Settings.loadClass(classLoader, entry.getKey(), key);
            final V value = valueConvertor.apply(entry.getValue());
            resolvedMappings.put(cls, value);
        }
        return resolvedMappings;
    }

    public void setVisibility(
            JsonAutoDetect.Visibility fieldVisibility,
            JsonAutoDetect.Visibility getterVisibility,
            JsonAutoDetect.Visibility isGetterVisibility,
            JsonAutoDetect.Visibility setterVisibility,
            JsonAutoDetect.Visibility creatorVisibility) {
        this.fieldVisibility = fieldVisibility;
        this.getterVisibility = getterVisibility;
        this.isGetterVisibility = isGetterVisibility;
        this.setterVisibility = setterVisibility;
        this.creatorVisibility = creatorVisibility;
    }

}
