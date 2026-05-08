
package cz.habarta.typescript.generator;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class ParentClassExclusionTest {

    private static class Base {
    }

    @SuppressWarnings("NullAway.Init")
    private static class Derived extends Base {
        public String name;
    }

    @Test
    public void testParentClassExclusion() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.mapClasses = ClassMapping.asClasses;
        settings.generateConstructors = true;
        settings.setExcludeFilter(List.of(Base.class.getName()), null);
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Derived.class));
        Assertions.assertTrue(!output.contains("super"));
        Assertions.assertTrue(!output.contains("extends"));
        Assertions.assertTrue(!output.contains("implements"));
    }

}
