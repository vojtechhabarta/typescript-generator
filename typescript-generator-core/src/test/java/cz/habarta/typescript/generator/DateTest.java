
package cz.habarta.typescript.generator;

import java.io.*;
import java.util.*;
import org.junit.*;


public class DateTest {

    @Test
    public void testDate() {
        TypeScriptGenerator.generateTypeScript(Arrays.asList(Dates.class), settings(DateMapping.asDate, "AsDate"), new File("target/test-date.d.ts"));
    }

    @Test
    public void testDateAsNumber() {
        TypeScriptGenerator.generateTypeScript(Arrays.asList(Dates.class), settings(DateMapping.asNumber, "AsNumber"), new File("target/test-dateAsNumber.d.ts"));
    }

    @Test
    public void testDateAsString() {
        TypeScriptGenerator.generateTypeScript(Arrays.asList(Dates.class), settings(DateMapping.asString, "AsString"), new File("target/test-dateAsString.d.ts"));
    }

    private static Settings settings(DateMapping mapDate, String namespace) {
        final Settings settings = new Settings();
        settings.jsonLibrary = JsonLibrary.jackson2;
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
