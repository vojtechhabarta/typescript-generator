
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.parser.*;
import java.util.*;
import org.junit.*;


public class ModelParserTest {

    @Test
    public void testClassDiscovery1() {
        final Model model = parseModel(RootClass1.class, null);
        Assert.assertEquals(2, model.getBeans().size());
        
    }

    @Test
    public void testClassDiscovery2() {
        final Model model = parseModel(RootClass2.class, null);
        Assert.assertEquals(2, model.getBeans().size());
    }

    @Test
    public void testClassDiscovery3() {
        final Model model = parseModel(RootClass3.class, null);
        Assert.assertEquals(3, model.getBeans().size());
    }

    @Test
    public void testClassDiscoveryExcludeTag() {
        final Model model = parseModel(RootClass3.class, Arrays.asList(Tag.class.getName()));
        Assert.assertEquals(2, model.getBeans().size());
    }

    @Test
    public void testClassDiscoveryExcludeNodeClassB() {
        final Model model = parseModel(RootClass3.class, Arrays.asList(NodeClassB.class.getName()));
        Assert.assertEquals(1, model.getBeans().size());
    }

    private Model parseModel(Class<?> rootClass, List<String> excludedClassNames) {
        final Settings settings = new Settings();
        settings.excludedClassNames = excludedClassNames;
        final ModelParser parser = new Jackson2Parser(settings, new TypeProcessor.Chain(
                new ExcludingTypeProcessor(settings.excludedClassNames),
                new DefaultTypeProcessor()
        ));
        return parser.parseModel(rootClass);
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
