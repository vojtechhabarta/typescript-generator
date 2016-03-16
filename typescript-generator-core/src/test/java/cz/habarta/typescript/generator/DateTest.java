
package cz.habarta.typescript.generator;

import java.io.*;
import java.util.*;
import org.junit.*;


public class DateTest {

    @Test
    public void testDate() {
        new TypeScriptGenerator(settings(DateMapping.asDate, "AsDate")).generateTypeScript(Input.from(Dates.class), Output.to(new File("target/test-date.d.ts")));
    }

    @Test
    public void testDateAsNumber() {
        new TypeScriptGenerator(settings(DateMapping.asNumber, "AsNumber")).generateTypeScript(Input.from(Dates.class), Output.to(new File("target/test-dateAsNumber.d.ts")));
    }

    @Test
    public void testDateAsString() {
        new TypeScriptGenerator(settings(DateMapping.asString, "AsString")).generateTypeScript(Input.from(Dates.class), Output.to(new File("target/test-dateAsString.d.ts")));
    }

    private static Settings settings(DateMapping mapDate, String namespace) {
        final Settings settings = TestUtils.settings();
        settings.namespace = namespace;
        settings.mapDate = mapDate;
        return settings;
    }

}

class Dates {
    public Date date;
    public List<Date> dateList;
    public Map<String, List<Date>> datesMap;
    public Date[] dates;
}
