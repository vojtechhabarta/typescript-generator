
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import cz.habarta.typescript.generator.compiler.ModelCompiler;
import cz.habarta.typescript.generator.emitter.TsModel;
import cz.habarta.typescript.generator.parser.Jackson2Parser;
import cz.habarta.typescript.generator.parser.Model;
import java.lang.reflect.*;
import java.util.*;
import org.hamcrest.CoreMatchers;
import org.junit.*;


public class ModelCompilerTest {

    @Test
    public void testEnum() throws Exception {
        final Settings settings = getTestSettings();
        final Type javaType = A.class.getField("directions").getGenericType();
        Assert.assertEquals("{ [index: string]: Direction }[]", TestUtils.compileType(settings, javaType).toString());
    }

    @Test
    public void testDate() throws Exception {
        final Settings settings = getTestSettings();
        final Type javaType = A.class.getField("timestamp").getGenericType();
        Assert.assertEquals("DateAsString", TestUtils.compileType(settings, javaType).toString());
    }

    @Test
    public void testExclusion() throws Exception {
        final Settings settings = getTestSettings(Direction.class.getName());
        final Type javaType = A.class.getField("directions").getGenericType();
        Assert.assertEquals("{ [index: string]: any }[]", TestUtils.compileType(settings, javaType).toString());
    }

    @Test
    public void testExclusionPattern() throws Exception {
        final Settings settings = TestUtils.settings();
        settings.setExcludeFilter(null, Arrays.asList("**Direction"));
        final Type javaType = A.class.getField("directions").getGenericType();
        Assert.assertEquals("{ [index: string]: any }[]", TestUtils.compileType(settings, javaType).toString());
    }

    @Test
    public void testIntermediateInterfacesWithoutTypeParams() throws Exception {
        final Settings settings = TestUtils.settings();

        final Jackson2Parser jacksonParser = new Jackson2Parser(settings, new DefaultTypeProcessor());
        final Model model = jacksonParser.parseModel(Implementation.class);
        final ModelCompiler modelCompiler = new TypeScriptGenerator(settings).getModelCompiler();

        final TsModel result = modelCompiler.javaToTypeScript(model);

        Assert.assertThat(
                result.getBean(WithoutTypeParam.class).getProperties().get(0).tsType,
                CoreMatchers.instanceOf(TsType.UnionType.class)
        );
    }

    @Test
    public void testIntermediateInterfacesWithTypeParams() throws Exception {
        final Settings settings = TestUtils.settings();

        final Jackson2Parser jacksonParser = new Jackson2Parser(settings, new DefaultTypeProcessor());
        final Model model = jacksonParser.parseModel(Implementation.class);
        final ModelCompiler modelCompiler = new TypeScriptGenerator(settings).getModelCompiler();

        final TsModel result = modelCompiler.javaToTypeScript(model);

        Assert.assertThat(
                result.getBean(WithTypeParam.class).getProperties().get(0).tsType,
                CoreMatchers.instanceOf(TsType.UnionType.class)
        );
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
    private static interface WithoutTypeParam {}

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
    private static interface WithTypeParam<T> {}

    private static class Implementation implements WithTypeParam<Integer>, WithoutTypeParam {}

    private static Settings getTestSettings(String... excludedClassNames) {
        final Settings settings = TestUtils.settings();
        settings.mapDate = DateMapping.asString;
        settings.setExcludeFilter(Arrays.asList(excludedClassNames), null);
        return settings;
    }

    private static enum Direction {
        North, East, South, West
    }

    private static class A {
        public List<Map<String, Direction>> directions;
        public Date timestamp;
    }

}
