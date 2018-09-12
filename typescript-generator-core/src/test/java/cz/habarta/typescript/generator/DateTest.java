
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.time.*;
import java.util.*;
import org.junit.*;


public class DateTest {

    @Test
    public void testDate_forJavaUtilDate() {
        final Settings settings = TestUtils.settings();
        settings.mapDate = DateMapping.asDate;
        final String dts = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Dates.class));
        Assert.assertTrue(dts.contains("date: Date;"));
        Assert.assertTrue(dts.contains("dateList: Date[];"));
        Assert.assertTrue(dts.contains("datesMap: { [index: string]: Date[] };"));
        Assert.assertTrue(dts.contains("dates: Date[];"));
    }

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
    public void testDateAsNumber_forJavaUtilDate() {
        final Settings settings = TestUtils.settings();
        settings.mapDate = DateMapping.asNumber;
        final String dts = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Dates.class));
        Assert.assertTrue(dts.contains("date: DateAsNumber;"));
        Assert.assertTrue(dts.contains("dateList: DateAsNumber[];"));
        Assert.assertTrue(dts.contains("datesMap: { [index: string]: DateAsNumber[] };"));
        Assert.assertTrue(dts.contains("dates: DateAsNumber[];"));
        Assert.assertTrue(dts.contains("type DateAsNumber = number;"));
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
    public void testDateAsString_forJavaUtilDate() {
        final Settings settings = TestUtils.settings();
        settings.mapDate = DateMapping.asString;
        final String dts = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Dates.class));
        Assert.assertTrue(dts.contains("date: DateAsString;"));
        Assert.assertTrue(dts.contains("dateList: DateAsString[];"));
        Assert.assertTrue(dts.contains("datesMap: { [index: string]: DateAsString[] };"));
        Assert.assertTrue(dts.contains("dates: DateAsString[];"));
        Assert.assertTrue(dts.contains("type DateAsString = string;"));
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
    public void testDateAsString_forJava8DateTime() {
        final Settings settings = TestUtils.settings();
        settings.mapDate = DateMapping.asString;
        final String dts = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Java8Dates.class));
        Assert.assertTrue(dts.contains("date: DateAsString;"));
        Assert.assertTrue(dts.contains("dateList: DateAsString[];"));
        Assert.assertTrue(dts.contains("datesMap: { [index: string]: DateAsString[] };"));
        Assert.assertTrue(dts.contains("dates: DateAsString[];"));
        Assert.assertTrue(dts.contains("type DateAsString = string;"));
    }

    @Test
    public void testJava8DateWithJackson2CustomSerialization() {
        final Settings settings = TestUtils.settings();
        settings.customTypeMappings = Collections.singletonMap("java.time.LocalDate", "[number, number, number]");
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Java8Jackson2Dates.class));
        Assert.assertTrue(output.contains("date: [number, number, number];"));
    }

    public static void main(String[] args) throws JsonProcessingException {
        final ObjectMapper objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        final Clock clock = Clock.fixed(Instant.parse("2017-09-02T19:11:00Z"), ZoneId.systemDefault());
        System.out.println(objectMapper.writeValueAsString(new Date()));
        System.out.println(objectMapper.writeValueAsString(Calendar.getInstance()));
        System.out.println(objectMapper.writeValueAsString(Instant.now(clock)));
        System.out.println(objectMapper.writeValueAsString(LocalDate.now(clock)));
        System.out.println(objectMapper.writeValueAsString(LocalDateTime.now(clock)));
        System.out.println(objectMapper.writeValueAsString(LocalTime.now(clock)));
        System.out.println(objectMapper.writeValueAsString(OffsetDateTime.now(clock)));
        System.out.println(objectMapper.writeValueAsString(OffsetTime.now(clock)));
        System.out.println(objectMapper.writeValueAsString(Year.now(clock)));
        System.out.println(objectMapper.writeValueAsString(YearMonth.now(clock)));
        System.out.println(objectMapper.writeValueAsString(ZonedDateTime.now(clock)));
    }

}

class Dates {
    public Date date;
    public List<Date> dateList;
    public Map<String, List<Date>> datesMap;
    public Date[] dates;
}

class JodaDates {
    public org.joda.time.DateTime date;
    public List<org.joda.time.DateTime> dateList;
    public Map<String, List<org.joda.time.DateTime>> datesMap;
    public org.joda.time.DateTime[] dates;
}

class Java8Dates {
    public LocalDateTime date;
    public List<LocalDateTime> dateList;
    public Map<String, List<LocalDateTime>> datesMap;
    public LocalDateTime[] dates;
}

class Java8Jackson2Dates {
    public LocalDate date;
}
