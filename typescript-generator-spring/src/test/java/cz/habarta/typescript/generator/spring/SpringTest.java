
package cz.habarta.typescript.generator.spring;

import cz.habarta.typescript.generator.Input;
import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.TestUtils;
import cz.habarta.typescript.generator.TypeScriptFileType;
import cz.habarta.typescript.generator.TypeScriptGenerator;
import cz.habarta.typescript.generator.util.Utils;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


public class SpringTest {

    @Test
    public void testAnnotationUtils() {
        final Method greetingMethod = getMethod(SpringTestApplication.GreetingController.class, "greeting");
        final RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(greetingMethod, RequestMapping.class);
        Assert.assertNotNull(mapping);
        Assert.assertEquals(0, mapping.method().length);
        Assert.assertEquals(1, mapping.path().length);
        Assert.assertEquals("/greeting", mapping.path()[0]);
    }

    private static Method getMethod(Class<?> cls, String methodName) {
        final Method greetingMethod = Utils.getAllMethods(cls).stream()
                .filter(method -> method.getName().equals(methodName))
                .findFirst()
                .get();
        return greetingMethod;
    }

    @Test
    public void testApplicationScan() {
        final Settings settings = TestUtils.settings();
        settings.generateSpringApplicationInterface = true;
        settings.scanSpringApplication = true;
        settings.classLoader = Thread.currentThread().getContextClassLoader();
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(SpringTestApplication.class));
        Assert.assertTrue(output.contains("interface RestApplication"));
        Assert.assertTrue(output.contains("greeting(queryParams?: { name?: string; count?: number; unnamed?: string; }): RestResponse<Greeting>"));
        Assert.assertTrue(output.contains("interface Greeting"));
    }

    @Test
    public void testPathParameters() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateSpringApplicationClient = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Controller1.class));
        Assert.assertTrue(output.contains("findPet(ownerId: number, petId: number): RestResponse<Pet>"));
        Assert.assertTrue(output.contains("uriEncoding`owners/${ownerId}/pets/${petId}`"));
        Assert.assertTrue(output.contains("interface Pet"));
    }

    @Test
    public void testQueryParameters() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateSpringApplicationClient = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Controller2.class));
        Assert.assertTrue(output.contains("echo(queryParams?: { message?: string; count?: number; }): RestResponse<string>"));
    }

    @Test
    public void testEntityParameter() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateSpringApplicationClient = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Controller3.class));
        Assert.assertTrue(output.contains("setEntity(data: Data1): RestResponse<void>"));
        Assert.assertTrue(output.contains("interface Data1"));
    }

    @Test
    public void testReturnType() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateSpringApplicationClient = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Controller4.class));
        Assert.assertTrue(output.contains("getEntity(): RestResponse<Data2>"));
        Assert.assertTrue(output.contains("interface Data2"));
    }

    @RestController
    @RequestMapping("/owners/{ownerId}")
    public static class Controller1 {
        @GetMapping("/pets/{petId}")
        public Pet findPet(
                @PathVariable("ownerId") Long ownerId,
                @PathVariable(name = "petId") Long petId
        ) {
            return null;
        }
    }

    @RestController
    public static class Controller2 {
        @RequestMapping("/echo")
        public String echo(
                @RequestParam("message") String message,
                @RequestParam(name = "count", defaultValue = "1") Integer count
        ) {
            return message;
        }
    }

    @RestController
    public static class Controller3 {
        @RequestMapping(path = "/data1", method = RequestMethod.PUT)
        public void setEntity(@RequestBody Data1 data) {
        }
    }

    @RestController
    public static class Controller4 {
        @RequestMapping(path = "/data2", method = RequestMethod.GET)
        public ResponseEntity<Data2> getEntity() {
            return null;
        }
    }

    public static class Pet {
    }

    public static class Data1 {
    }

    public static class Data2 {
    }

    @Test
    public void testUnwrapping() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateSpringApplicationClient = true;
        settings.customTypeMappings.put("cz.habarta.typescript.generator.spring.SpringTest$Wrapper<T>", "Unwrap<T>");
        settings.importDeclarations.add("import { Unwrap } from './unwrap'");
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ControllerWithWrapper.class));
        Assert.assertTrue(output.contains("getEntity(): RestResponse<Unwrap<string>>"));
    }

    @Test
    public void testUnwrappingNew() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateSpringApplicationClient = true;
        settings.customTypeMappings.put("cz.habarta.typescript.generator.spring.SpringTest$Wrapper<T>", "T");
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ControllerWithWrapper.class));
        Assert.assertTrue(output.contains("getEntity(): RestResponse<string>"));
    }

    @RestController
    public static class ControllerWithWrapper {
        @RequestMapping(path = "/data", method = RequestMethod.GET)
        public Wrapper<String> getEntity() {
            return null;
        }
    }

    public static class Wrapper<T> {
        public T value;
    }

    @Test
    public void testGenericController() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateSpringApplicationClient = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ConcreteGenerticController.class));
        Assert.assertTrue(output.contains("post(input: string): RestResponse<number>"));
    }

    @RestController
    public static abstract class AbstractGenerticController<T, R> {
        @PostMapping("/generic")
        public R post(@RequestBody T input) {
            return map(input);
        }

        abstract protected R map(T input);
    }

    public static class ConcreteGenerticController extends AbstractGenerticController<String, Integer> {
        protected Integer map(String input) {
            return input.length();
        }
    }

    @Test
    public void testPageableController() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateSpringApplicationClient = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(PageableController.class));
        Assert.assertTrue(output.contains(" post(queryParams?: { page?: number; size?: number; sort?: string; }): RestResponse<Page<string>>"));
    }

    @RestController
    public static abstract class PageableController {
        @GetMapping("/pageable")
        public Page<String> post(Pageable page) {
            return null;
        }
    }

    @Test
    public void testDoubleGenericController() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateSpringApplicationClient = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(DoubleGenericController.class));
        Assert.assertTrue(output.contains(" get(): RestResponse<string[]>"));
    }

    @RestController
    public class DoubleGenericController  {
        @GetMapping("/generic2")
        public ResponseEntity<List<String>> get () {
            return ResponseEntity.ok(Arrays.asList( "a" , "b" , "c" ));
        }
    }
}
