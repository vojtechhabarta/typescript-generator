
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.util.Utils;
import java.util.*;


public class TsHelper {

    private final List<String> lines;

    public TsHelper(List<String> lines) {
        this.lines = lines;
    }

    public static TsHelper loadFromResource(String resourceName) {
        return new TsHelper(Utils.readLines(TsHelper.class.getResourceAsStream(resourceName)));
    }

    public List<String> getLines() {
        return lines;
    }

}
