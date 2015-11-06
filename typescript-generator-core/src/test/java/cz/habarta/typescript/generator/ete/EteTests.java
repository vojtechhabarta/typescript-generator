package cz.habarta.typescript.generator.ete;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.junit.Test;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.common.base.Optional;

import cz.habarta.typescript.generator.JavaToTypescriptTypeParser;
import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.TsType;
import cz.habarta.typescript.generator.TypeScriptGenerator;


public class EteTests {
    @Test
    public void genericTest() throws FileNotFoundException, ClassNotFoundException {
        File f = new File("src/test/java/cz/habarta/typescript/generator/ete/EteTestsData.txt");
        Scanner s = new Scanner(f);
        String nextLine = s.nextLine();
        assertEquals("=== Begin expected declarations", nextLine);
        Map<Class<?>, String> classToExpectedDeclaration = Maps.newHashMap();
        while (!((nextLine = s.nextLine()).equals("=== End expected declarations"))) {
            String className = nextLine;
            Class<?> clazz = getClassForName(className);
            String declaration = "";
            do {
                nextLine = s.nextLine();
                declaration += nextLine;
                if (!nextLine.equals("}")) {
                    declaration += "\n";
                }
            } while(!nextLine.equals("}"));
            classToExpectedDeclaration.put(clazz, declaration);
        }

        int testNum = 0;
        while (s.hasNextLine()) {
            System.out.println("Running test: " + testNum);
            String x = s.nextLine(); // start test
            if (!x.equals("=== Begin test")) {
                break;
            }
            String y = s.nextLine(); // start input
            List<Class<?>> inputClasses = Lists.newArrayList();
            while (!(nextLine = s.nextLine()).equals("=== End input classes")) {
                inputClasses.add(getClassForName(nextLine));
            }
            s.nextLine(); // start output
            List<Class<?>> outputClasses = Lists.newArrayList();
            while (!(nextLine = s.nextLine()).equals("=== End output classes")) {
                outputClasses.add(getClassForName(nextLine));
            }
            String actual = generateClasses(inputClasses);
            String expected = "";
            for (Class<?> expectedOutputClazz : outputClasses) {
                expected += "\n";
                expected += classToExpectedDeclaration.get(expectedOutputClazz);
                expected += "\n";
            }
            assertEquals(expected, actual);
            s.nextLine(); // end test
            testNum++;
        }
    }

    private static String generateClasses(List<Class<?>> inputs) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Settings s = new Settings();

        s.customTypeParser = new JavaToTypescriptTypeParser() {
            @Override
            public TsType typeFromJava(Type javaType, JavaToTypescriptTypeParser fallback) {
                if (javaType instanceof ParameterizedType) {
                    ParameterizedType param = (ParameterizedType) javaType;
                    if (param.getRawType() == Optional.class) {
                        Type arg = param.getActualTypeArguments()[0];
                        TsType inner = fallback.typeFromJava(arg, null).getOptionalReference();
                        return inner;
                    }
                }
                return null;
            }
        };

        TypeScriptGenerator.generateTypeScript(inputs, s, out);
        return new String(out.toByteArray());
    }

    private static Class<?> getClassForName(String name) throws ClassNotFoundException {
        return Class.forName("cz.habarta.typescript.generator.ete.EteTests$" + name);
    }

    public static class Class00 {
        public String x;
    }

    public static class Class01 {
        public Optional<String> x;
    }

    public static enum Class02 {
        A, B, C
    }

    public static class Class03<T> {
        public List<T> x;
    }

    public static class Class04<T, V> {
        public Optional<Map<T, V>> x;
        public List<T> y;
    }

    public static class Class05 {
        public Class03<Class00> x;
    }

    public static class Class06 {
        public Class06 x;
    }

    public static class Class07 {
        public Class08 x;
    }

    public static class Class08 {
        public Class07 x;
    }

    public static class Class09<T> {
        public Class04<T, Class00> x;
    }
}
