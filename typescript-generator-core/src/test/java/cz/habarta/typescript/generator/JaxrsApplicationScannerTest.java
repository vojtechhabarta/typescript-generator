
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.core.type.*;
import cz.habarta.typescript.generator.parser.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import javax.activation.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.xml.bind.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import org.junit.*;


public class JaxrsApplicationScannerTest<T> {

    @Test
    public void testScanJaxrsApplicationTypes() throws Exception {
        final List<SourceType<Type>> sourceTypes = new JaxrsApplicationScanner().scanJaxrsApplication(new TestApplication());
        final List<Type> types = new ArrayList<>();
        for (SourceType<Type> sourceType : sourceTypes) {
            types.add(sourceType.type);
        }
        final List<Type> expectedTypes = Arrays.asList(
                A.class,
                new TypeReference<List<B>>(){}.getType(),
                C.class,
                new TypeReference<List<D>>(){}.getType(),
                List.class,
                E.class,
                new TypeReference<List<F>>(){}.getType(),
                G.class,
                new TypeReference<Map<String, H>>(){}.getType(),
                I.class
        );
        assertHasSameItems(expectedTypes, types);
    }

    @Test
    public void testScanJaxrsApplicationClasses() throws Exception {
        final List<SourceType<Type>> types = new JaxrsApplicationScanner().scanJaxrsApplication(new TestApplication());
        final Model model = new TypeScriptGenerator().getModelParser().parseModel(types);
        final ArrayList<Class<?>> classes = new ArrayList<>();
        for (BeanModel beanModel : model.getBeans()) {
            classes.add(beanModel.getBeanClass());
        }
        final List<Class<?>> expectedClasses = Arrays.asList(
                A.class,
                B.class,
                C.class,
                D.class,
                E.class,
                F.class,
                G.class,
                H.class,
                I.class
        );
        assertHasSameItems(expectedClasses, classes);
    }

    private static <T> void assertHasSameItems(Collection<? extends T> expected, Collection<? extends T> actual) {
        for (T value : expected) {
            Assert.assertTrue("Value '" + value + "' is missing in " + actual, actual.contains(value));
        }
        for (T value : actual) {
            Assert.assertTrue("Value '" + value + "' not expected.", expected.contains(value));
        }
    }

    private static class TestApplication extends Application {
        @Override
        public Set<Class<?>> getClasses() {
            return new LinkedHashSet<Class<?>>(Arrays.asList(
                    TestResource1.class
            ));
        }
    }

    @Path("test")
    private static class TestResource1 {
        @GET
        public void getVoid() {
        }
        @GET
        public Response getResponse() {
            return null;
        }
        @GET
        public GenericEntity<A> getA() {
            return null;
        }
        @GET
        public GenericEntity<List<B>> getB() {
            return null;
        }
        @GET
        public C getC() {
            return null;
        }
        @GET
        public List<D> getD() {
            return null;
        }
        @SuppressWarnings("rawtypes")
        @GET
        public List getRawList() {
            return null;
        }
        @GET
        @Path("e")
        public E getE() {
            return null;
        }
        @Path("f")
        public SubResource1 getSubResource1() {
            return null;
        }
        @POST
        public void setG(G g) {
        }
        @POST
        public void setHs(Map<String, H> hs) {
        }
        @POST
        public void setI(
                @MatrixParam("") String matrixParam,
                @QueryParam("") String queryParam,
                @PathParam("") String pathParam,
                @CookieParam("") String cookieParam,
                @HeaderParam("") String headerParam,
                @Context String context,
                @FormParam("") String formParam,
                I entityI) {
        }
        @POST
        public void setStandardEntity(byte[] value) {}
        @POST
        public void setStandardEntity(String value) {}
        @POST
        public void setStandardEntity(InputStream value) {}
        @POST
        public void setStandardEntity(Reader value) {}
        @POST
        public void setStandardEntity(File value) {}
        @POST
        public void setStandardEntity(DataSource value) {}
        @POST
        public void setStandardEntity(Source value) {}
        @POST
        public void setStandardEntity(DOMSource value) {}
        @POST
        public void setStandardEntity(JAXBElement<?> value) {}
        @POST
        public void setStandardEntity(MultivaluedMap<String,String> value) {}
        @POST
        public void setStandardEntity(StreamingOutput value) {}
        @POST
        public void setStandardEntity(Boolean value) {}
        @POST
        public void setStandardEntity(Character value) {}
        @POST
        public void setStandardEntity(Number value) {}
        @POST
        public void setStandardEntity(Integer value) {}
        @POST
        public void setStandardEntity(int value) {}
    }

    private static class SubResource1 {
        @GET
        public List<F> getFs() {
            return null;
        }
    }

    private static class A {}
    private static class B {}
    private static class C {}
    private static class D {}
    private static class E {}
    private static class F {}
    private static class G {}
    private static class H {}
    private static class I {}

}
