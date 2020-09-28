
package cz.habarta.typescript.generator.library;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import cz.habarta.typescript.generator.DateMapping;
import cz.habarta.typescript.generator.Input;
import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.TestUtils;
import cz.habarta.typescript.generator.TypeScriptGenerator;
import cz.habarta.typescript.generator.util.Utils;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.MonthDay;
import org.joda.time.Period;
import org.joda.time.YearMonth;
import org.junit.Assert;
import org.junit.Test;


public class JodaTest {

    @Test
    public void testDate_forJodaDateTime() {
        final Settings settings = TestUtils.settings();
        settings.customTypeMappings.put("org.joda.time.DateTime", "Date");
        final String dts = new TypeScriptGenerator(settings).generateTypeScript(Input.from(JodaDates.class));
        Assert.assertTrue(dts.contains("date: Date;"));
        Assert.assertTrue(dts.contains("dateList: Date[];"));
        Assert.assertTrue(dts.contains("datesMap: { [index: string]: Date[] };"));
        Assert.assertTrue(dts.contains("dates: Date[];"));
    }

    @Test
    public void testDateAsNumber_forJodaDateTime() {
        final Settings settings = TestUtils.settings();
        settings.customTypeMappings.put("org.joda.time.DateTime", "number");
        final String dts = new TypeScriptGenerator(settings).generateTypeScript(Input.from(JodaDates.class));
        Assert.assertTrue(dts.contains("date: number;"));
        Assert.assertTrue(dts.contains("dateList: number[];"));
        Assert.assertTrue(dts.contains("datesMap: { [index: string]: number[] };"));
        Assert.assertTrue(dts.contains("dates: number[];"));
        Assert.assertTrue(!dts.contains("type DateAsNumber = number;"));
    }

    @Test
    public void testDateAsString_forJodaDateTime() {
        final Settings settings = TestUtils.settings();
        settings.customTypeMappings.put("org.joda.time.DateTime", "string");
        final String dts = new TypeScriptGenerator(settings).generateTypeScript(Input.from(JodaDates.class));
        Assert.assertTrue(dts.contains("date: string;"));
        Assert.assertTrue(dts.contains("dateList: string[];"));
        Assert.assertTrue(dts.contains("datesMap: { [index: string]: string[] };"));
        Assert.assertTrue(dts.contains("dates: string[];"));
        Assert.assertTrue(!dts.contains("type DateAsString = string;"));
    }

    @Test
    public void testDateAsString_forJodaDateTime_usingDataLibrary() {
        final Settings settings = TestUtils.settings();
        settings.additionalDataLibraries = Arrays.asList("joda");
        settings.mapDate = DateMapping.asString;
        final String dts = new TypeScriptGenerator(settings).generateTypeScript(Input.from(JodaDates.class));
        Assert.assertTrue(dts.contains("date: DateAsString;"));
        Assert.assertTrue(dts.contains("dateList: DateAsString[];"));
        Assert.assertTrue(dts.contains("datesMap: { [index: string]: DateAsString[] };"));
        Assert.assertTrue(dts.contains("dates: DateAsString[];"));
        Assert.assertTrue(dts.contains("type DateAsString = string;"));
    }

    @Test
    public void testJodaInJackson() throws Exception {
        final ObjectMapper objectMapper = Utils.getObjectMapper();
        objectMapper.registerModule(new JodaModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        final String json = objectMapper.writeValueAsString(new JodaSerializedClasses());
//        System.out.println(json);
    }

    @Test
    public void testJodaLibrary() {
        final Settings settings = TestUtils.settings();
        settings.additionalDataLibraries = Arrays.asList("joda");
        settings.mapDate = DateMapping.asString;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(JodaSerializedClasses.class));
        Assert.assertTrue(output.contains("dateTime: DateAsString;"));
        Assert.assertTrue(output.contains("dateTimeZone: string;"));
        Assert.assertTrue(output.contains("duration: number;"));
        Assert.assertTrue(output.contains("instant: DateAsString;"));
        Assert.assertTrue(output.contains("localDateTime: DateAsString;"));
        Assert.assertTrue(output.contains("localDate: DateAsString;"));
        Assert.assertTrue(output.contains("localTime: DateAsString;"));
        Assert.assertTrue(output.contains("period: string;"));
        Assert.assertTrue(output.contains("interval: string;"));
        Assert.assertTrue(output.contains("monthDay: string;"));
        Assert.assertTrue(output.contains("yearMonth: string;"));
        Assert.assertTrue(output.contains("dateMidnight: DateAsString;"));
    }

}

class JodaDates {
    public org.joda.time.DateTime date;
    public List<org.joda.time.DateTime> dateList;
    public Map<String, List<org.joda.time.DateTime>> datesMap;
    public org.joda.time.DateTime[] dates;
}

class JodaSerializedClasses {
    public DateTime dateTime = DateTime.now();
    public DateTimeZone dateTimeZone = DateTimeZone.UTC;
    public Duration duration = Duration.ZERO;
    public Instant instant = Instant.now();
    public LocalDateTime localDateTime = LocalDateTime.now();
    public LocalDate localDate = LocalDate.now();
    public LocalTime localTime = LocalTime.now();
    public Period period = Period.ZERO;
    public Interval interval = new Interval(Instant.now(), Instant.now().plus(10));
    public MonthDay monthDay = MonthDay.now();
    public YearMonth yearMonth = YearMonth.now();
    public DateMidnight dateMidnight = DateMidnight.now();
}
