
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.util.Utils;
import java.lang.reflect.Type;
import java.util.*;


public class ExcludingTypeProcessor implements TypeProcessor {

    private final Set<String> excludedClassNames = new LinkedHashSet<>();

    public ExcludingTypeProcessor(List<String> excludedClassNames) {
        if (excludedClassNames != null) {
            this.excludedClassNames.addAll(excludedClassNames);
        }
    }

    @Override
    public Result processType(Type javaType, Context context) {
        final Class<?> rawClass = Utils.getRawClassOrNull(javaType);
        if (rawClass != null && excludedClassNames.contains(rawClass.getName())) {
            return new Result(TsType.Any);
        }
        return null;
    }

}
