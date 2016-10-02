
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.core.type.TypeReference;
import cz.habarta.typescript.generator.parser.*;
import java.lang.reflect.Type;
import java.util.*;
import org.junit.*;


public class ModelParserTest {

    @Test
    public void testClassDiscovery1() {
        final Model model = parseModel(RootClass1.class);
        Assert.assertEquals(2, model.getBeans().size());
        
    }

    @Test
    public void testClassDiscovery2() {
        final Model model = parseModel(RootClass2.class);
        Assert.assertEquals(2, model.getBeans().size());
    }

    @Test
    public void testClassDiscovery3() {
        final Model model = parseModel(RootClass3.class);
        Assert.assertEquals(3, model.getBeans().size());
    }

    @Test
    public void testClassDiscoveryExcludeNodeClassA() {
        final Model model = parseModel(RootClass1.class, NodeClassA.class.getName());
        Assert.assertEquals(1, model.getBeans().size());
    }

    @Test
    public void testClassDiscoveryExcludeTag() {
        final Model model = parseModel(RootClass3.class, Tag.class.getName());
        Assert.assertEquals(2, model.getBeans().size());
    }

    @Test
    public void testClassDiscoveryExcludeNodeClassB() {
        final Model model = parseModel(RootClass3.class, NodeClassB.class.getName());
        Assert.assertEquals(1, model.getBeans().size());
    }

    @Test
    public void testExcludedInputDirectly() {
        final Model model = parseModel(RootClass3.class, RootClass3.class.getName());
        Assert.assertEquals(0, model.getBeans().size());
    }

    @Test
    public void testExcludedInputInList() {
        final Model model = parseModel(new TypeReference<List<RootClass3>>() {}.getType(), RootClass3.class.getName());
        Assert.assertEquals(0, model.getBeans().size());
    }

    private Model parseModel(Type type, String... excludedClassNames) {
        final Settings settings = new Settings();
        settings.setExcludeFilter(Arrays.asList(excludedClassNames), null);
        final ModelParser parser = new Jackson2Parser(settings, new TypeProcessor.Chain(
                new ExcludingTypeProcessor(settings.getExcludeFilter()),
                new DefaultTypeProcessor()
        ));
        final Model model = parser.parseModel(type);
        return model;
    }

}

class RootClass1 {
    public NodeClassA node;
}

class RootClass2 {
    public List<NodeClassA> nodes;
}

class NodeClassA {
    public String name;
}

class RootClass3 {
    public Map<String, NodeClassB> nodes;
}

class NodeClassB {
    public String name;
    public NodeClassB leftNode;
    public NodeClassB rightNode;
    public Tag[] tags;
}

class Tag {
}
