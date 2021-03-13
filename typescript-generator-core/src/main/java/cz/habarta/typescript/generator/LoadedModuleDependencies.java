
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.habarta.typescript.generator.emitter.InfoJson;
import cz.habarta.typescript.generator.util.Pair;
import cz.habarta.typescript.generator.util.Utils;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;


public class LoadedModuleDependencies {

    private final Map<String/*javaClass*/, Pair<ModuleDependency, String/*namespacedName*/>> classMappings = new LinkedHashMap<>();

    public LoadedModuleDependencies(Settings settings, List<ModuleDependency> dependencies) {
        if (dependencies == null) {
            return;
        }
        final ObjectMapper objectMapper = Utils.getObjectMapper();
        final Map<String, ModuleDependency> importFromMap = new LinkedHashMap<>();
        final Map<String, ModuleDependency> importAsMap = new LinkedHashMap<>();
        final Map<String, ModuleDependency> globalTypeNames = new LinkedHashMap<>();
        for (ModuleDependency dependency : dependencies) {
            try {
                final Function<String, String> reportNullParameter = parameterName ->
                        String.format("Missing required configuration parameter '%s' in module dependency: %s", parameterName, dependency);
                if (dependency.global) {
                    if (dependency.importFrom != null) {
                        throw new RuntimeException(String.format(
                                "'importFrom' parameter is only applicable when 'global' is not set to 'true' (at module dependency %s).", dependency));
                    }
                    if (dependency.importAs != null) {
                        throw new RuntimeException(String.format(
                                "'importAs' parameter is only applicable when 'global' is not set to 'true' (at module dependency %s).", dependency));
                    }
                } else {
                    Objects.requireNonNull(dependency.importFrom, () -> reportNullParameter.apply("importFrom"));
                    Objects.requireNonNull(dependency.importAs, () -> reportNullParameter.apply("importAs"));
                }
                Objects.requireNonNull(dependency.infoJson, () -> reportNullParameter.apply("infoJson"));
                if (settings.generateNpmPackageJson) {
                    Objects.requireNonNull(dependency.npmPackageName, () -> reportNullParameter.apply("npmPackageName"));
                    Objects.requireNonNull(dependency.npmVersionRange, () -> reportNullParameter.apply("npmVersionRange"));
                } else {
                    if (dependency.npmPackageName != null) {
                        throw new RuntimeException(String.format(
                                "'npmPackageName' parameter is only applicable when 'generateNpmPackageJson' is set to 'true' (at module dependency %s).", dependency));
                    }
                    if (dependency.npmVersionRange != null) {
                        throw new RuntimeException(String.format(
                                "'npmVersionRange' parameter is only applicable when 'generateNpmPackageJson' is set to 'true' (at module dependency %s).", dependency));
                    }
                }

                TypeScriptGenerator.getLogger().info(String.format(
                        "Loading %s module info from: %s", dependency.toShortString(), dependency.infoJson));

                if (!dependency.global) {
                    final ModuleDependency importFromConflict = importFromMap.put(dependency.importFrom, dependency);
                    if (importFromConflict != null) {
                        throw new RuntimeException(String.format("Duplicate module '%s'", dependency.importFrom));
                    }

                    final ModuleDependency importAsConflict = importAsMap.put(dependency.importAs, dependency);
                    if (importAsConflict != null) {
                        throw new RuntimeException(String.format("Import identifier '%s' already used for module '%s'", dependency.importAs, importAsConflict.importFrom));
                    }
                }

                final InfoJson infoJson = objectMapper.readValue(dependency.infoJson, InfoJson.class);
                for (InfoJson.ClassInfo classInfo : infoJson.classes) {
                    final Pair<ModuleDependency, String> presentMapping = classMappings.get(classInfo.javaClass);
                    if (presentMapping != null) {
                        TypeScriptGenerator.getLogger().warning(String.format(
                                "Java class '%s' already present in '%s'", classInfo.javaClass, presentMapping.getValue1().infoJson));
                    } else {
                        classMappings.put(classInfo.javaClass, Pair.of(dependency, classInfo.typeName));
                    }
                    final ModuleDependency presentTypeName = globalTypeNames.get(classInfo.typeName);
                    if (presentTypeName != null) {
                        throw new RuntimeException(String.format(
                                "Duplicate TypeScript global name '%s', declared in '%s' and also '%s'", classInfo.typeName, presentTypeName.infoJson, dependency.infoJson));
                    } else {
                        globalTypeNames.put(classInfo.typeName, dependency);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public Pair<String/*module*/, String/*namespacedName*/> getFullName(Class<?> cls) {
        final Pair<ModuleDependency, String> mapping = classMappings.get(cls.getName());
        if (mapping != null) {
            return Pair.of(mapping.getValue1().importAs, mapping.getValue2());
        }
        return null;
    }

}
