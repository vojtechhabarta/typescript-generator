package cz.habarta.typescript.generator.util;

import java.util.ArrayList;
import java.util.List;

public final class DeprecationUtils {

    public static final String DEPRECATED = "@deprecated";

    public static String convertToComment(Deprecated deprecated) {
        // support for java 9+ syntax
        String since = Utils.getAnnotationElementValue(deprecated, "since", String.class);
        Boolean forRemoval = Utils.getAnnotationElementValue(deprecated, "forRemoval", Boolean.class);

        List<String> additional = new ArrayList<>();
        if (since != null && !since.isEmpty()) {
            additional.add("since: " + since);
        }
        if (forRemoval != null && forRemoval) {
            additional.add("forRemoval: true");
        }
        return additional.isEmpty() ? DEPRECATED : (DEPRECATED + " " + String.join("; ", additional));
    }
}
