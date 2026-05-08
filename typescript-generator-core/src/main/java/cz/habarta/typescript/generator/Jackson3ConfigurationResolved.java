
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;


public class Jackson3ConfigurationResolved {

    public JsonAutoDetect.@Nullable Visibility fieldVisibility;
    public JsonAutoDetect.@Nullable Visibility getterVisibility;
    public JsonAutoDetect.@Nullable Visibility isGetterVisibility;
    public JsonAutoDetect.@Nullable Visibility setterVisibility;
    public JsonAutoDetect.@Nullable Visibility creatorVisibility;
    public @Nullable Map<Class<?>, JsonFormat.Shape> shapeConfigOverrides;
    public boolean enumsUsingToString;
    public boolean disableObjectIdentityFeature;
    @SuppressWarnings("rawtypes")
    public @Nullable Map<Class<? extends ValueSerializer>, String> serializerTypeMappings;
    @SuppressWarnings("rawtypes")
    public @Nullable Map<Class<? extends ValueDeserializer>, String> deserializerTypeMappings;
    public @Nullable Class<?> view;

    public static Jackson3ConfigurationResolved from(Jackson3Configuration configuration, ClassLoader classLoader) {
        final Jackson3ConfigurationResolved resolved = new Jackson3ConfigurationResolved();
        resolved.fieldVisibility = configuration.fieldVisibility;
        resolved.getterVisibility = configuration.getterVisibility;
        resolved.isGetterVisibility = configuration.isGetterVisibility;
        resolved.setterVisibility = configuration.setterVisibility;
        resolved.creatorVisibility = configuration.creatorVisibility;
        resolved.fieldVisibility = configuration.fieldVisibility;
        resolved.shapeConfigOverrides = resolveClassMappings(
            configuration.shapeConfigOverrides, "shapeConfigOverride", classLoader, Object.class, JsonFormat.Shape::valueOf);
        resolved.enumsUsingToString = configuration.enumsUsingToString;
        resolved.disableObjectIdentityFeature = configuration.disableObjectIdentityFeature;
        resolved.deserializerTypeMappings = resolveClassMappings(
            configuration.deserializerTypeMappings, "deserializerTypeMapping", classLoader, ValueDeserializer.class, Function.identity());
        resolved.serializerTypeMappings = resolveClassMappings(
            configuration.serializerTypeMappings, "serializerTypeMapping", classLoader, ValueSerializer.class, Function.identity());
        resolved.view = configuration.view != null ? Settings.loadClass(classLoader, configuration.view, Object.class) : null;
        return resolved;
    }

    private static <C, V> @Nullable Map<Class<? extends C>, V> resolveClassMappings(
        @Nullable List<String> mappings,
        String mappingName,
        ClassLoader classLoader,
        Class<? extends C> key,
        Function<String, V> valueConvertor
    ) {
        if (mappings == null) {
            return null;
        }
        final Map<Class<? extends C>, V> resolvedMappings = new LinkedHashMap<>();
        final Map<String, String> mappingsMap = Settings.convertToMap(mappings, mappingName);
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
        JsonAutoDetect.Visibility creatorVisibility
    ) {
        this.fieldVisibility = fieldVisibility;
        this.getterVisibility = getterVisibility;
        this.isGetterVisibility = isGetterVisibility;
        this.setterVisibility = setterVisibility;
        this.creatorVisibility = creatorVisibility;
    }

}
