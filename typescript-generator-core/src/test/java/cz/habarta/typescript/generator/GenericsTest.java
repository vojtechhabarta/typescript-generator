package cz.habarta.typescript.generator;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.Assert.*;
import org.junit.*;


public class GenericsTest {

    @Test
    public void testDefaultGenerics() throws Exception {
        final Settings settings = new Settings();
        settings.noFileComment = true;

        final StringWriter output = new StringWriter();
        new TypeScriptGenerator(settings).generateTypeScript(Arrays.asList(A.class), output);
        final String actual = output.toString().trim();
        final String nl = settings.newline;
        final String expected =
                "interface A {" + nl +
                "    x: A;" + nl +
                "    y: A;" + nl +
                "    z: A;" + nl +
                "}";
        assertEquals(expected, actual);
    }

    @Test
    public void testAdvancedGenerics() throws Exception {
        final Settings settings = new Settings();
        settings.addTypeNamePrefix = "I";
        settings.noFileComment = true;
        settings.customTypeProcessor = new TypeProcessor() {
            @Override
            public TypeProcessor.Result processType(Type javaType, TypeProcessor.Context context) {
                if (javaType instanceof TypeVariable) {
                    final TypeVariable<?> typeVariable = (TypeVariable) javaType;
                    return new Result(new TsType.BasicType(typeVariable.getName()));
                }
                if (javaType instanceof Class) {
                    final Class<?> javaClass = (Class<?>) javaType;
                    if (javaClass.getTypeParameters().length > 0) {
                        return processGenericClass(javaClass, false, javaClass.getTypeParameters(), context);
                    }
                }
                if (javaType instanceof ParameterizedType) {
                    final ParameterizedType parameterizedType = (ParameterizedType) javaType;
                    if (parameterizedType.getRawType() instanceof Class) {
                        final Class<?> javaClass = (Class<?>) parameterizedType.getRawType();
                        return processGenericClass(javaClass, true, parameterizedType.getActualTypeArguments(), context);
                    }
                }
                return null;
            }
            private Result processGenericClass(Class<?> rawType, boolean processRawType, Type[] typeArguments, TypeProcessor.Context context) {
                if (!Collection.class.isAssignableFrom(rawType) && !Map.class.isAssignableFrom(rawType)) {
                    final List<Class<?>> discoveredClasses = new ArrayList<>();
                    // raw type
                    final String rawTsTypeName = context.getMappedName(rawType);
                    discoveredClasses.add(rawType);
                    // type arguments
                    final List<TsType> tsTypeArguments = new ArrayList<>();
                    for (Type typeArgument : typeArguments) {
                        final TypeProcessor.Result typeArgumentResult = context.processType(typeArgument);
                        tsTypeArguments.add(typeArgumentResult.getTsType());
                        discoveredClasses.addAll(typeArgumentResult.getDiscoveredClasses());
                    }
                    // result
                    final GenericStructuralType type = new GenericStructuralType(rawTsTypeName, tsTypeArguments);
                    return new Result(type, discoveredClasses);
                }
                return null;
            }
        };

        final StringWriter output = new StringWriter();
        new TypeScriptGenerator(settings).generateEmbeddableTypeScript(Arrays.asList(A.class), output, true, 0);
        final String actual = output.toString().trim();
        final String nl = settings.newline;
        final String expected =
                "export interface IA<U, V> {" + nl +
                "    x: IA<string, string>;" + nl +
                "    y: IA<IA<string, IB>, string[]>;" + nl +
                "    z: IA<{ [index: string]: V }, number[]>;" + nl +
                "}" + nl +
                "" + nl +
                "export interface IB {" + nl +
                "}";
        assertEquals(expected, actual);
        final ModelCompiler compiler = new TypeScriptGenerator(settings).getModelCompiler();
        assertEquals("IA<string, string>", compiler.typeFromJava(A.class.getField("x").getGenericType()).toString());
        assertEquals("IA<IA<string, IB>, string[]>", compiler.typeFromJava(A.class.getField("y").getGenericType()).toString());
        assertEquals("IA<{ [index: string]: V }, number[]>", compiler.typeFromJava(A.class.getField("z").getGenericType()).toString());
    }

    private static class GenericStructuralType extends TsType.StructuralType {

        public final List<TsType> typeArguments;

        public GenericStructuralType(String name, List<TsType> typeArguments) {
            super(name);
            this.typeArguments = typeArguments;
        }

        @Override
        public java.lang.String toString() {
            return name + "<" + ModelCompiler.join(typeArguments, ", ") + ">";
        }

    }

    class A<U,V> {
        public A<String, String> x;
        public A<A<String, B>, List<String>> y;
        public A<Map<String, V>, Set<Integer>> z;
    }

    class B {
    }

}
