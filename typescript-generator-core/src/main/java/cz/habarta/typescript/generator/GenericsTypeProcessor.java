
package cz.habarta.typescript.generator;

import java.lang.reflect.Type;
import kotlin.reflect.KType;


/**
 * @deprecated now the functionality is part of the core
 */
@Deprecated()
public class GenericsTypeProcessor implements TypeProcessor {

    @Override
    public Result processType(Type javaType, Context context) {
        return processType(javaType, null, context);
    }

    @Override
    public Result processType(Type javaType, KType kType, Context context) {
        return null;
    }

}
