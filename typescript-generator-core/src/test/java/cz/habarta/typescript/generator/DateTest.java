
package cz.habarta.typescript.generator;

import java.io.*;
import java.util.*;
import org.junit.*;


public class DateTest {

    @Test
    public void testDate() {
        final String dts = new TypeScriptGenerator(settings(DateMapping.asDate, "AsDate")).generateTypeScript(Input.from(Dates.class));
        Assert.assertTrue(dts.contains("date: Date;"));
        Assert.assertTrue(dts.contains("dateList: Date[];"));
        Assert.assertTrue(dts.contains("datesMap: { [index: string]: Date[] };"));
        Assert.assertTrue(dts.contains("dates: Date[];"));
    }

    @Test
    public void testDateAsNumber() {
        final String dts = new TypeScriptGenerator(settings(DateMapping.asNumber, "AsNumber")).generateTypeScript(Input.from(Dates.class));
        Assert.assertTrue(dts.contains("date: DateAsNumber;"));
        Assert.assertTrue(dts.contains("dateList: DateAsNumber[];"));
        Assert.assertTrue(dts.contains("datesMap: { [index: string]: DateAsNumber[] };"));
        Assert.assertTrue(dts.contains("dates: DateAsNumber[];"));
        Assert.assertTrue(dts.contains("type DateAsNumber = number;"));
    }

    @Test
    public void testDateAsString() {
        final String dts = new TypeScriptGenerator(settings(DateMapping.asString, "AsString")).generateTypeScript(Input.from(Dates.class));
        Assert.assertTrue(dts.contains("date: DateAsString;"));
        Assert.assertTrue(dts.contains("dateList: DateAsString[];"));
        Assert.assertTrue(dts.contains("datesMap: { [index: string]: DateAsString[] };"));
        Assert.assertTrue(dts.contains("dates: DateAsString[];"));
        Assert.assertTrue(dts.contains("type DateAsString = string;"));
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
