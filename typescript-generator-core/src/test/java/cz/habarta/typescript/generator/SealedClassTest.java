
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class SealedClassTest {

    // TODO uncomment on Java 17

    // @Test
    // public void testObjectMapper() throws JsonProcessingException {
    //     final ObjectMapper objectMapper = new ObjectMapper();
    //     objectMapper.registerModule(new SealedClassesModule());
    //     final Shape shape = objectMapper.readValue(
    //         """
    //         {
    //             "type": "circle",
    //             "radius": 42
    //         }
    //         """,
    //         Shape.class
    //     );
    //     Assertions.assertTrue(shape instanceof Circle);
    //     Assertions.assertEquals(42, ((Circle)shape).radius);
    // }

    // @Test
    // public void testSealedClassWithModule() throws JsonProcessingException {
    //     final Settings settings = TestUtils.settings();
    //     settings.jackson2Modules.add(SealedClassesModule.class);
    //     final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Shape.class));
    //     Assertions.assertTrue(output.contains("circle"));
    // }

    // @Test
    // public void testSealedClassWithoutModule() throws JsonProcessingException {
    //     final Settings settings = TestUtils.settings();
    //     final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Shape.class));
    //     Assertions.assertFalse(output.contains("circle"));
    // }

    // @JsonTypeInfo(use = Id.NAME, property = "type")
    // private static abstract sealed class Shape {
    // }

    // @JsonTypeName("circle")
    // private static final class Circle extends Shape {
    //     public double radius;
    // }

    // public static class SealedClassesModule extends SimpleModule {
    //     @Override
    //     public void setupModule(SetupContext context) {
    //         context.appendAnnotationIntrospector(new SealedClassesAnnotationIntrospector());
    //     }
    // }

    // public static class SealedClassesAnnotationIntrospector extends JacksonAnnotationIntrospector {
    //     @Override
    //     public List<NamedType> findSubtypes(Annotated a) {
    //         if (a.getAnnotated() instanceof Class<?> cls && cls.isSealed()) {
    //             final Class<?>[] permittedSubclasses = cls.getPermittedSubclasses();
    //             if (permittedSubclasses.length > 0) {
    //                 return Arrays.stream(permittedSubclasses).map(NamedType::new).toList();
    //             }
    //         }
    //         return null;
    //     }
    // }

}
