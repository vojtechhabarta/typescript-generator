
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.parser.*;
import java.util.*;
import org.junit.*;


public class ModelParserTest {

    @Test
    public void testClassDiscovery1() {
        testClassDiscovery(RootClass1.class, 2);
    }

    @Test
    public void testClassDiscovery2() {
        testClassDiscovery(RootClass2.class, 2);
    }

    @Test
    public void testClassDiscovery3() {
        testClassDiscovery(RootClass3.class, 3);
    }

    private void testClassDiscovery(Class<?> rootClass, int expectedCount) {
        final ModelParser parser = Jackson2ParserTest.getJackson2Parser();
        final Model model = parser.parseModel(rootClass);
        Assert.assertEquals(expectedCount, model.getBeans().size());
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
