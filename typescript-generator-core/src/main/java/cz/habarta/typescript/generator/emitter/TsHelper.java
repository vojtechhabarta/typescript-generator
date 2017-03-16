
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.util.Utils;
import java.util.*;


public class TsHelper {

    private final List<String> lines;

    public TsHelper(List<String> lines) {
        this.lines = lines;
    }

    public static TsHelper loadFromResource(String resourceName) {
        final String text = Utils.readString(TsHelper.class.getResourceAsStream(resourceName));
        return new TsHelper(Utils.splitMultiline(text, false));
    }

    public List<String> getLines() {
        return lines;
    }

}
