
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class DateTest {

    @Test
    public void testDate_forJavaUtilDate() {
        final Settings settings = TestUtils.settings();
        settings.mapDate = DateMapping.asDate;
        final String dts = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Dates.class));
        Assertions.assertTrue(dts.contains("date: Date;"));
        Assertions.assertTrue(dts.contains("dateList: Date[];"));
        Assertions.assertTrue(dts.contains("datesMap: { [index: string]: Date[] };"));
        Assertions.assertTrue(dts.contains("dates: Date[];"));
        Assertions.assertTrue(dts.contains("calendar: Date;"));
    }

    @Test
    public void testDateAsNumber_forJavaUtilDate() {
        final Settings settings = TestUtils.settings();
        settings.mapDate = DateMapping.asNumber;
        final String dts = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Dates.class));
        Assertions.assertTrue(dts.contains("date: DateAsNumber;"));
        Assertions.assertTrue(dts.contains("dateList: DateAsNumber[];"));
        Assertions.assertTrue(dts.contains("datesMap: { [index: string]: DateAsNumber[] };"));
        Assertions.assertTrue(dts.contains("dates: DateAsNumber[];"));
        Assertions.assertTrue(dts.contains("calendar: DateAsNumber;"));
        Assertions.assertTrue(dts.contains("type DateAsNumber = number;"));
    }

    @Test
    public void testDateAsString_forJavaUtilDate() {
        final Settings settings = TestUtils.settings();
        settings.mapDate = DateMapping.asString;
        final String dts = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Dates.class));
        Assertions.assertTrue(dts.contains("date: DateAsString;"));
        Assertions.assertTrue(dts.contains("dateList: DateAsString[];"));
        Assertions.assertTrue(dts.contains("datesMap: { [index: string]: DateAsString[] };"));
        Assertions.assertTrue(dts.contains("dates: DateAsString[];"));
        Assertions.assertTrue(dts.contains("calendar: DateAsString;"));
        Assertions.assertTrue(dts.contains("type DateAsString = string;"));
    }

    @Test
    public void testDateAsString_forJava8DateTime() {
        final Settings settings = TestUtils.settings();
        settings.mapDate = DateMapping.asString;
        final String dts = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Java8Dates.class));
        Assertions.assertTrue(dts.contains("date: DateAsString;"));
        Assertions.assertTrue(dts.contains("dateList: DateAsString[];"));
        Assertions.assertTrue(dts.contains("datesMap: { [index: string]: DateAsString[] };"));
        Assertions.assertTrue(dts.contains("dates: DateAsString[];"));
        Assertions.assertTrue(dts.contains("type DateAsString = string;"));
    }

    @Test
    public void testJava8DateWithJackson2CustomSerialization() {
        final Settings settings = TestUtils.settings();
        settings.customTypeMappings = Collections.singletonMap("java.time.LocalDate", "[number, number, number]");
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Java8Jackson2Dates.class));
        Assertions.assertTrue(output.contains("date: [number, number, number];"));
    }

    public static void main(String[] args) throws JsonProcessingException {
        final ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
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
    public Calendar calendar;
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
