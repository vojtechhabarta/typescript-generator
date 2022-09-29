package cz.habarta.typescript.generator.util;

import java.util.ArrayList;
import java.util.List;

public final class DeprecationUtils {

    public static final String DEPRECATED = "@deprecated";

    public static String convertToComment(Deprecated deprecated) {
        String since = deprecated.since();
        Boolean forRemoval = deprecated.forRemoval();

        List<String> additional = new ArrayList<>();
        if (since != null && !since.isEmpty()) {
            additional.add("since " + since);
        }
        if (forRemoval != null && forRemoval) {
            additional.add("for removal");
        }
        return additional.isEmpty() ? DEPRECATED : (DEPRECATED + " " + String.join(", ", additional));
    }

}
