
package cz.habarta.typescript.generator.parser;

import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.TypeProcessor;
import cz.habarta.typescript.generator.util.Utils;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public abstract class RestApplicationParser {

    protected final Settings settings;
    protected final Predicate<String> isClassNameExcluded;
    protected final TypeProcessor commonTypeProcessor;
    protected final RestApplicationModel model;

    public static abstract class Factory {

        public TypeProcessor getSpecificTypeProcessor() {
            return null;
        }

        public abstract RestApplicationParser create(Settings settings, TypeProcessor commonTypeProcessor);

    }

    public RestApplicationParser(Settings settings, TypeProcessor commonTypeProcessor, RestApplicationModel model) {
        this.settings = settings;
        this.isClassNameExcluded = settings.getExcludeFilter();
        this.commonTypeProcessor = commonTypeProcessor;
        this.model = model;
    }

    public RestApplicationModel getModel() {
        return model;
    }

    protected abstract Result tryParse(SourceType<?> sourceType);

    public static class Result {
        public List<SourceType<Type>> discoveredTypes;
        public Result() {
            discoveredTypes = new ArrayList<>();
        }
        public Result(List<SourceType<Type>> discoveredTypes) {
            this.discoveredTypes = discoveredTypes;
        }
    }

    protected void foundType(Result result, Type type, Class<?> usedInClass, String usedInMember) {
        if (!commonTypeProcessor.isTypeExcluded(type, null, settings)) {
            result.discoveredTypes.add(new SourceType<>(type, usedInClass, usedInMember));
        }
    }

    protected static class ResourceContext {
        public final Class<?> rootResource;
        public final String path;
        public final Map<String, Type> pathParamTypes;

        public ResourceContext(Class<?> rootResource, String path) {
            this(rootResource, path, new LinkedHashMap<String, Type>());
        }

        private ResourceContext(Class<?> rootResource, String path, Map<String, Type> pathParamTypes) {
            this.rootResource = rootResource;
            this.path = path;
            this.pathParamTypes = pathParamTypes;
        }

        public ResourceContext subPath(String subPath) {
            return new ResourceContext(rootResource, Utils.joinPath(path, subPath), pathParamTypes);
        }

        public ResourceContext subPathParamTypes(Map<String, Type> subPathParamTypes) {
            final Map<String, Type> newPathParamTypes = new LinkedHashMap<>();
            newPathParamTypes.putAll(pathParamTypes);
            if (subPathParamTypes != null) {
                newPathParamTypes.putAll(subPathParamTypes);
            }
            return new ResourceContext(rootResource, path, newPathParamTypes);
        }
    }

}
