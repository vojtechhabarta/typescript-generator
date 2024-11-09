
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("unused")
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

    /**
     * Checks that the custom type mapping works for non-nested generic parameters.
     * That is, each type parameter is not generic by itself.
     */
    @Test
    public void testSimpleGenericParameter() {
        class ClassWithNonNestedGenericTypes {
            public List<String> stringList;
            public List<BigDecimal> bigDecimalList;
        }

        final Settings settings = TestUtils.settings();
        settings.quotes = "'";
        settings.customTypeMappings.put("java.util.List<BigDecimal>", "number[]");
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ClassWithNonNestedGenericTypes.class));
        System.out.println(output);

        assertTrue(output.contains("stringList: string[];"));
        assertTrue(output.contains("bigDecimalList: number[];"));
    }

    /**
     * Checks that the custom type mapping works for nested generic parameters.
     * That is, a type parameter is generic by itself.
     */
    @Test
    public void testNestedGenericParameter() {
        class ClassWithNestedGenericTypes {
            public List<String> stringList;
            public List<List<BigDecimal>> bigDecimalMatrix;
        }

        final Settings settings = TestUtils.settings();
        settings.quotes = "'";
        settings.customTypeMappings.put("java.util.List<java.util.List<BigDecimal>>", "number[][]");
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ClassWithNestedGenericTypes.class));
        System.out.println(output);

        assertTrue(output.contains("stringList: string[];"));
        assertTrue(output.contains("bigDecimalMatrix: number[][];"));
    }

    private static class CustomTypesUsage {
        public Date date1;
        public Calendar calendar1;
    }

    @Test
    public void testEnumAsMap() throws Exception {
//        final ObjectMapper objectMapper = new ObjectMapper();
//        final String json = objectMapper.writeValueAsString(MyEnum.MY_FIRST_VALUE);
//        System.out.println(json);

        final Settings settings = TestUtils.settings();
        settings.customTypeMappings = Collections.singletonMap("cz.habarta.typescript.generator.CustomTypeMappingTest$MyEnum", "{ code: string, definition: string }");
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(MyInterfUsingEnum.class));
        assertTrue(output.contains("someValue: { code: string, definition: string }"));
    }

    /**
     * Tests that custom mapping a superclass to a primitive doesn't cause errors.
     */
    @Test
    public void testSuperTypeString() throws Exception {
        final Settings settings = TestUtils.settings();
        settings.customTypeMappings = Collections.singletonMap("cz.habarta.typescript.generator.CustomTypeMappingTest$BaseCustomMapping", "string");
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(InterfaceUsingSubCustomMapping.class));
        assertTrue(output.contains("sub: SubCustomMapping;"));
    }


    @JsonSerialize(using = CodedValueSerializer.class)
    public interface CodedValue {
        String getCode();
        String getDefinition();
    }

    public enum MyEnum implements CodedValue {

        MY_FIRST_VALUE("A0", "Some description");

        private final String code;
        private final String definition;

        private MyEnum(String code, String definition) {
            this.code = code;
            this.definition = definition;
        }

        @Override
        public String getCode() {
            return code;
        }

        @Override
        public String getDefinition() {
            return definition;
        }
    }

    public interface MyInterfUsingEnum {
        public MyEnum getSomeValue();
    }

    public static class CodedValueSerializer extends StdSerializer<CodedValue> {

        private static final long serialVersionUID = 1L;

        public CodedValueSerializer() {
            super(CodedValue.class);
        }

        public CodedValueSerializer(Class<CodedValue> t) {
            super(t);
        }

        @Override
        public void serialize(CodedValue value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            gen.writeStringField("code", value.getCode());
            gen.writeStringField("definition", value.getDefinition());
            gen.writeEndObject();
        }
    }


    class BaseCustomMapping {}
    class SubCustomMapping extends BaseCustomMapping {}
    interface InterfaceUsingSubCustomMapping {
        SubCustomMapping getSub();
    }

    @Test
    public void testGenericClassWithCustomMapping() {
        final Settings settings = TestUtils.settings();
        settings.customTypeMappings = Collections.singletonMap("cz.habarta.typescript.generator.CustomTypeMappingTest$GenericClass<D>", "CustomGenericClass<D>");
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(GenericClass.class));
        Assertions.assertEquals("", output);
    }

    static class GenericClass<D> {
        public D type;
    }

    @Test
    public void testBigInt() throws Exception {
        final Settings settings = TestUtils.settings();
        settings.customTypeMappings = Collections.singletonMap("long", "BigInt");
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(BigIntClass.class));
        Assertions.assertTrue(output.contains("bigNumber: BigInt;"));
    }

    private static class BigIntClass {
        public long bigNumber = Long.MAX_VALUE;
    }

}
