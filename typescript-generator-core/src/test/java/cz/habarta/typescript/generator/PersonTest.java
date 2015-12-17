
package cz.habarta.typescript.generator;

import java.io.File;
import org.junit.Test;


public class PersonTest {
    
    @Test
    public void test() {
        new TypeScriptGenerator().generateTypeScript(Input.from(Person.class), new File("target/person.d.ts"));
    }

}
