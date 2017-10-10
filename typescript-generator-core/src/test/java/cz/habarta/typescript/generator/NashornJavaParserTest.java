package cz.habarta.typescript.generator;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Test;

import cz.habarta.typescript.generator.compiler.ModelCompiler;
import cz.habarta.typescript.generator.emitter.Emitter;
import cz.habarta.typescript.generator.emitter.TsModel;
import cz.habarta.typescript.generator.parser.Model;
import cz.habarta.typescript.generator.parser.NashornJavaParser;

public class NashornJavaParserTest {
    
    public static class MyDummyBeanSuper {
        
        public String methodInSuper(String namedArg1, int otherArg) {
            return "";
        }
        
    }
    
    public static class MyDummyBean extends MyDummyBeanSuper {
        
        private String privateStringProperty;
        private boolean privateBooleanProperty;
        
        /* Property getter - string */
        public String getStringProperty() {
            return privateStringProperty;
        }
        
        /* Property getter - boolean */
        public boolean isBoolenProperty() {
            return privateBooleanProperty;
        }
        
        public String someMethod(String nameArg, int someNumArg, List<String> arg3) {
            return "";
        }        
        
        public Iterator<OtherDummy> getIteratorMethod(String x) {
            return null;
        }

        public Iterator<OtherDummy> getIteratorGetter() {
            return null;
        }
        
        public void callbackMethod(Consumer<String> consumer) {
            consumer.accept("Hello");
        }    
    }
    
    public static class OtherDummy {
        public String getName() {
            return "Hello";
        }
    }
    
    @Test
    public void testMyDummyBean() {
        final StringWriter stringWriter = new StringWriter();
        Settings settings = new Settings();        
        
        settings.namespace = "Java";
        settings.outputKind = TypeScriptOutputKind.global;
        settings.mapPackagesToNamespaces = true;     
        settings.jsonLibrary = JsonLibrary.nashorn;
        
        TypeProcessor typeProcessor = getTypeProcessor(settings);
        NashornJavaParser parser = new NashornJavaParser(settings, typeProcessor);
        ModelCompiler modelCompiler = new ModelCompiler(settings, typeProcessor);
        Emitter emitter = new Emitter(settings);
        final Class<?> bean = MyDummyBean.class;
        final Model model = parser.parseModel(bean);
        final TsModel tsModel = modelCompiler.javaToTypeScript(model);
        emitter.emit(tsModel, stringWriter, "dummy", true, false, 0);

        System.out.println("model="+model);
        System.out.println("tsModel="+tsModel);
        System.out.println(stringWriter.toString());
    }    
    
    @Test
    public void testArrayList() {
        final StringWriter stringWriter = new StringWriter();
        Settings settings = new Settings();        
        
        settings.namespace = "Java";
        settings.outputKind = TypeScriptOutputKind.global;
        settings.mapPackagesToNamespaces = true;
        settings.jsonLibrary = JsonLibrary.nashorn;
        
        TypeProcessor typeProcessor = getTypeProcessor(settings);
        NashornJavaParser parser = new NashornJavaParser(settings, typeProcessor);
        ModelCompiler modelCompiler = new ModelCompiler(settings, typeProcessor);
        Emitter emitter = new Emitter(settings);
        final Class<?> bean = ArrayList.class;
        final Model model = parser.parseModel(bean);
        final TsModel tsModel = modelCompiler.javaToTypeScript(model);
        emitter.emit(tsModel, stringWriter, "dummy", true, false, 0);

        System.out.println("model="+model);
        System.out.println("tsModel="+tsModel);
        System.out.println(stringWriter.toString());
    }
    
 
    private TypeProcessor getTypeProcessor(Settings settings) {
        final List<TypeProcessor> processors = new ArrayList<>();
        processors.add(new ExcludingTypeProcessor(settings.getExcludeFilter()));
        if (settings.customTypeProcessor != null) {
            processors.add(settings.customTypeProcessor);
        }
        processors.add(new CustomMappingTypeProcessor(settings.customTypeMappings));
        processors.add(new DefaultTypeProcessor());
        return new TypeProcessor.Chain(processors);
    }    
    
}