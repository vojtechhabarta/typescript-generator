
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.util.Utils;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;


public class ExcludingTypeProcessor implements TypeProcessor {

    private final Predicate<String> excludeFilter;

    public ExcludingTypeProcessor(List<String> excludedTypes) {
        this(new Predicate<String>() {
            final Set<String> excludedTypesSet = excludedTypes != null ? new LinkedHashSet<>(excludedTypes) : Collections.emptySet();
            @Override
            public boolean test(String typeName) {
                return excludedTypesSet.contains(typeName);
            }
        });
    }

    public ExcludingTypeProcessor(Predicate<String> excludeFilter) {
        this.excludeFilter = excludeFilter;
    }

    @Override
    public Result processType(Type javaType, Context context) {
        final Class<?> rawClass = Utils.getRawClassOrNull(javaType);
        if (rawClass != null && excludeFilter.test(rawClass.getName())) {
            return new Result(TsType.Any);
        }
        return null;
    }

}
