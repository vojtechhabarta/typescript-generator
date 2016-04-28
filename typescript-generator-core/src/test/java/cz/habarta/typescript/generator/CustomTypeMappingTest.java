
package cz.habarta.typescript.generator;

import java.util.*;
import static org.junit.Assert.*;
import org.junit.Test;


public class CustomTypeMappingTest {

    @Test
    public void test() {
        final Settings settings = TestUtils.settings();
        settings.quotes = "'";
        settings.referencedFiles.add("../src/test/ts/my-custom-types.d.ts");
        settings.importDeclarations.add("import * as myModule from '../src/test/ts/my-module.d.ts'");
        settings.customTypeMappings.put("java.util.Date", "MyDate");
        settings.customTypeMappings.put("java.util.Calendar", "myModule.MyCalendar");
//        new TypeScriptGenerator(settings).generateTypeScript(Input.from(CustomTypesUsage.class), Output.to(new File("target/CustomTypeMappingTest.d.ts")));
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(CustomTypesUsage.class));
        assertTrue(output.contains("/// <reference path='../src/test/ts/my-custom-types.d.ts' />"));
        assertTrue(output.contains("import * as myModule from '../src/test/ts/my-module.d.ts';"));
        assertTrue(output.contains("date1: MyDate;"));
        assertTrue(output.contains("calendar1: myModule.MyCalendar;"));
    }

    private static class CustomTypesUsage {
        public Date date1;
        public Calendar calendar1;
    }

}
