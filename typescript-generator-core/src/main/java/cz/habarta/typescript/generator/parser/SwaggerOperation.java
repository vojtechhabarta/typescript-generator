
package cz.habarta.typescript.generator.parser;

import java.lang.reflect.Type;
import java.util.List;
import org.jspecify.annotations.Nullable;


public class SwaggerOperation {
    public @Nullable Type responseType;
    public @Nullable List<SwaggerResponse> possibleResponses;
    public boolean hidden;
    public @Nullable String comment;
}
