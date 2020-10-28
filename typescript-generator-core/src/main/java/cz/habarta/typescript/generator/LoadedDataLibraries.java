
package cz.habarta.typescript.generator;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;


public class LoadedDataLibraries {

    public final List<Class<?>> stringClasses;
    public final List<Class<?>> numberClasses;
    public final List<Class<?>> booleanClasses;
    public final List<Class<?>> dateClasses;
    public final List<Class<?>> anyClasses;
    public final List<Class<?>> voidClasses;
    public final List<Class<?>> listClasses;
    public final List<Class<?>> mapClasses;
    public final List<Class<?>> optionalClasses;
    public final List<Class<?>> wrapperClasses;
    public final List<Settings.CustomTypeMapping> typeMappings;
    public final List<Settings.CustomTypeAlias> typeAliases;

    public LoadedDataLibraries() {
        this(empty(), empty(), empty(), empty(), empty(), empty(), empty(), empty(), empty(), empty(), empty(), empty());
    }

    private static <T> List<T> empty() {
        return Collections.emptyList();
    }

    public LoadedDataLibraries(
            List<Class<?>> stringClasses,
            List<Class<?>> numberClasses,
            List<Class<?>> booleanClasses,
            List<Class<?>> dateClasses,
            List<Class<?>> anyClasses,
            List<Class<?>> voidClasses,
            List<Class<?>> listClasses,
            List<Class<?>> mapClasses,
            List<Class<?>> optionalClasses,
            List<Class<?>> wrapperClasses,
            List<Settings.CustomTypeMapping> typeMappings,
            List<Settings.CustomTypeAlias> typeAliases
    ) {
        this.stringClasses = stringClasses;
        this.numberClasses = numberClasses;
        this.booleanClasses = booleanClasses;
        this.dateClasses = dateClasses;
        this.anyClasses = anyClasses;
        this.voidClasses = voidClasses;
        this.listClasses = validateNumberOfGenericParameters(listClasses, 1);
        this.mapClasses = validateNumberOfGenericParameters(mapClasses, 2);
        this.optionalClasses = validateNumberOfGenericParameters(optionalClasses, 1);
        this.wrapperClasses = validateNumberOfGenericParameters(wrapperClasses, 1);
        this.typeMappings = typeMappings;
        this.typeAliases = typeAliases;
    }

    private static List<Class<?>> validateNumberOfGenericParameters(List<Class<?>> classes, int required) {
        for (Class<?> cls : classes) {
            if (cls.getTypeParameters().length != required) {
                throw new RuntimeException(String.format(
                        "Data library class '%s' is required to have %d generic type parameters but it has %d",
                        cls.getName(), required, cls.getTypeParameters().length));
            }
        }
        return classes;
    }
    
    public static LoadedDataLibraries join(LoadedDataLibraries... jsons) {
        return join(Arrays.asList(jsons));
    }

    public static LoadedDataLibraries join(List<LoadedDataLibraries> jsons) {
        return new LoadedDataLibraries(
                joinMappedLists(jsons, j -> j.stringClasses),
                joinMappedLists(jsons, j -> j.numberClasses),
                joinMappedLists(jsons, j -> j.booleanClasses),
                joinMappedLists(jsons, j -> j.dateClasses),
                joinMappedLists(jsons, j -> j.anyClasses),
                joinMappedLists(jsons, j -> j.voidClasses),
                joinMappedLists(jsons, j -> j.listClasses),
                joinMappedLists(jsons, j -> j.mapClasses),
                joinMappedLists(jsons, j -> j.optionalClasses),
                joinMappedLists(jsons, j -> j.wrapperClasses),
                joinMappedLists(jsons, j -> j.typeMappings),
                joinMappedLists(jsons, j -> j.typeAliases)
        );
    }

    private static <T, M> List<M> joinMappedLists(List<T> list, Function<T, List<M>> mapper) {
        return list.stream()
                .filter(Objects::nonNull)
                .map(mapper)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

}
