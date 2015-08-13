
package cz.habarta.typescript.generator;

import java.io.File;
import java.util.Arrays;

import org.junit.Test;

public class TypeScriptGeneratorTest {

	@Test
	public void test() {
		TypeScriptGenerator.generateTypeScript(Arrays.asList(DummyBean.class), new Settings(),
				new File("target/testNoModule.d.ts"));

		final Settings settings = new Settings();
		settings.moduleName = "Test";
		settings.declareEnums = true;
		TypeScriptGenerator.generateTypeScript(Arrays.asList(DummyBean.class, DummyEnum.class), settings,
				new File("target/testWithModule.d.ts"));
	}

}
