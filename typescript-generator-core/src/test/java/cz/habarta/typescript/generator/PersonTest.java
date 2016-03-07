
package cz.habarta.typescript.generator;

import java.io.File;
import org.junit.Test;


public class PersonTest {
    
    @Test
    public void test() {
        new TypeScriptGenerator(TestUtils.settings()).generateTypeScript(Input.from(Person.class), Output.to(new File("target/person.d.ts")));
    }

}
