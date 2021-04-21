
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.core.type.TypeReference;
import cz.habarta.typescript.generator.compiler.ModelCompiler;
import cz.habarta.typescript.generator.parser.BeanModel;
import cz.habarta.typescript.generator.parser.JaxrsApplicationParser;
import cz.habarta.typescript.generator.parser.Model;
import cz.habarta.typescript.generator.parser.SourceType;
import cz.habarta.typescript.generator.type.JGenericArrayType;
import cz.habarta.typescript.generator.type.JTypeWithNullability;
import io.github.classgraph.ClassGraph;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.MatrixParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import javax.activation.DataSource;
import javax.xml.bind.JAXBElement;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Assert;
import org.junit.Test;


@SuppressWarnings("unused")
public class JaxrsApplicationTest {

    @Test
    public void testReturnedTypesFromApplication() {
        final List<SourceType<Type>> sourceTypes = JaxrsApplicationScanner.scanJaxrsApplication(TestApplication.class, null);
        List<Type> types = getTypes(sourceTypes);
        final List<Type> expectedTypes = Arrays.<Type>asList(
                TestApplication.class,
                TestResource1.class
        );
        assertHasSameItems(expectedTypes, types);
    }

    @Test
    public void testReturnedTypesFromResource() {
        JaxrsApplicationParser jaxrsApplicationParser = createJaxrsApplicationParser(TestUtils.settings());
        final JaxrsApplicationParser.Result result = jaxrsApplicationParser.tryParse(new SourceType<>(TestResource1.class));
        Assert.assertNotNull(result);
        List<Type> types = getTypes(result.discoveredTypes);
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
                I.class,
                JGenericArrayType.of(J[].class),
                // types handled by DefaultTypeProcessor
                String.class, Boolean.class, Character.class, Number.class, Integer.class, int.class, void.class
        );
        assertHasSameItems(expectedTypes, types);
    }

    @Test
    public void testWithParsingWithExplicitApplication() {
        final List<SourceType<Type>> sourceTypes = JaxrsApplicationScanner.scanJaxrsApplication(TestApplication.class, null);
        testWithParsing(sourceTypes, true);
    }

    @Test
    public void testWithParsingWithDefaultApplication() {
        final List<SourceType<Type>> sourceTypes = JaxrsApplicationScanner.scanAutomaticJaxrsApplication(new ClassGraph().enableAllInfo().scan(), null);
        testWithParsing(sourceTypes, false);
    }

    private void testWithParsing(List<SourceType<Type>> types, boolean exactMatch) {
        final Model model = new TypeScriptGenerator(TestUtils.settings()).getModelParser().parseModel(types);
        final ArrayList<Class<?>> classes = new ArrayList<>();
        for (BeanModel beanModel : model.getBeans()) {
            classes.add(beanModel.getOrigin());
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
                I.class,
                J.class
        );
        if (exactMatch) {
            assertHasSameItems(expectedClasses, classes);
        } else {
            Assert.assertTrue(classes.containsAll(expectedClasses));
        }
    }

    @Test
    public void testExcludedResource() {
        final Predicate<String> excludeFilter = Settings.createExcludeFilter(Arrays.asList(
                TestResource1.class.getName()
        ), null);
        final List<SourceType<Type>> sourceTypes = JaxrsApplicationScanner.scanJaxrsApplication(TestApplication.class, excludeFilter);
        final List<Type> types = getTypes(sourceTypes);
        Assert.assertEquals(1, types.size());
        Assert.assertTrue(getTypes(sourceTypes).contains(TestApplication.class));
    }

    @Test
    public void testExcludedType() {
        final Settings settings = TestUtils.settings();
        settings.setExcludeFilter(Arrays.asList(
                A.class.getName(),
                J.class.getName()
        ), null);
        final JaxrsApplicationParser jaxrsApplicationParser = createJaxrsApplicationParser(settings);
        final JaxrsApplicationParser.Result result = jaxrsApplicationParser.tryParse(new SourceType<>(TestResource1.class));
        Assert.assertNotNull(result);
        Assert.assertTrue(!getTypes(result.discoveredTypes).contains(A.class));
        Assert.assertTrue(getTypes(result.discoveredTypes).contains(JGenericArrayType.of(J[].class)));
    }

    private static JaxrsApplicationParser createJaxrsApplicationParser(Settings settings) {
        final TypeProcessor typeProcessor = new TypeScriptGenerator(settings).getCommonTypeProcessor();
        final JaxrsApplicationParser jaxrsApplicationParser = new JaxrsApplicationParser(settings, typeProcessor);
        return jaxrsApplicationParser;
    }

    private List<Type> getTypes(final List<? extends SourceType<? extends Type>> sourceTypes) {
        final List<Type> types = new ArrayList<>();
        for (SourceType<? extends Type> sourceType : sourceTypes) {
            types.add(JTypeWithNullability.removeNullability(sourceType.type));
        }
        return types;
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
            return new LinkedHashSet<>(Arrays.asList(
                    TestResource1.class
            ));
        }
    }

    @Path("test")
    static class TestResource1 {
        @GET
        public void getVoid() {
        }
        @GET
        public Response getResponse() {
            return null;
        }
        @GET
        @Path("a")
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
                @Suspended AsyncResponse suspendedParam,
                @HeaderParam("") String headerParam,
                @Context String context,
                @FormParam("") String formParam,
                I entityI) {
        }
        @POST
        @ApiOperation(value = "async", response = String.class)
        public void setAsync(
                @Suspended AsyncResponse suspendedParam
        ) {
        }
        @POST
        public void setJs(J[] js) {
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
    private static class J {}

    @Test
    public void basicInterfaceTest() {
        final Settings settings = TestUtils.settings();
        settings.generateJaxrsApplicationInterface = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(TestResource1.class));
        final String errorMessage = "Unexpected output: " + output;
        Assert.assertTrue(errorMessage, output.contains("interface RestApplication"));
        Assert.assertTrue(errorMessage, output.contains("getA(): RestResponse<A>;"));
        Assert.assertTrue(errorMessage, output.contains("type RestResponse<R> = Promise<R>;"));
        Assert.assertTrue(errorMessage, !output.contains("function uriEncoding"));
        Assert.assertTrue(errorMessage, output.contains("setAsync(): RestResponse<string>"));
    }

    @Test
    public void complexInterfaceTest() {
        final Settings settings = TestUtils.settings();
        settings.generateJaxrsApplicationInterface = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(OrganizationApplication.class));
        final String errorMessage = "Unexpected output: " + output;
        Assert.assertTrue(errorMessage, output.contains("type RestResponse<R> = Promise<R>;"));
        Assert.assertTrue(errorMessage, output.contains("interface Organization"));
        Assert.assertTrue(errorMessage, output.contains("interface OrganizationApplication"));
        Assert.assertTrue(errorMessage, output.contains("HTTP GET /api/organizations/{ organizationCode : [a-z]+ }/{organizationId}"));
        Assert.assertTrue(errorMessage, output.contains("getOrganization(organizationCode: string, organizationId: number): RestResponse<Organization>;"));
        Assert.assertTrue(errorMessage, output.contains("searchOrganizations(queryParams?: { name?: string; \"search-limit\"?: number; }): RestResponse<Organization[]>;"));
        Assert.assertTrue(errorMessage, output.replace("arg1", "organization").contains("setOrganization(organizationCode: string, organizationId: number, organization: Organization): RestResponse<void>;"));
        Assert.assertTrue(errorMessage, output.contains("HTTP GET /api/people/{personId}/address/{address-id}"));
        Assert.assertTrue(errorMessage, output.contains("getAddress(personId: number, addressId: number): RestResponse<Address>;"));
        Assert.assertTrue(errorMessage, output.contains("HTTP GET /api/people/{personId}"));
        Assert.assertTrue(errorMessage, output.contains("getPerson(personId: number): RestResponse<Person>;"));
    }

    @Test
    public void methodNameConflictTest() {
        final Settings settings = TestUtils.settings();
        settings.generateJaxrsApplicationInterface = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(NameConflictResource.class));
        final String errorMessage = "Unexpected output: " + output;
        Assert.assertTrue(errorMessage, output.contains("interface RestApplication"));
        Assert.assertTrue(errorMessage, output.replace("arg0", "person").contains("person$POST$conflict(person: Person): RestResponse<Person>;"));
        Assert.assertTrue(errorMessage, output.contains("person$GET$conflict(): RestResponse<Person>;"));
        Assert.assertTrue(errorMessage, output.contains("person$GET$conflict_search(queryParams?: { search?: string; }): RestResponse<Person>;"));
        Assert.assertTrue(errorMessage, output.contains("person$GET$conflict_personId(personId: number): RestResponse<Person>;"));
    }

    @Test
    public void customizationTest() {
        final Settings settings = TestUtils.settings();
        settings.generateJaxrsApplicationInterface = true;
        settings.restResponseType = "AxiosPromise";
        settings.restOptionsType = "AxiosRequestConfig";
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(OrganizationApplication.class));
        final String errorMessage = "Unexpected output: " + output;
        Assert.assertTrue(errorMessage, output.contains("type RestResponse<R> = AxiosPromise;"));
        Assert.assertTrue(errorMessage, output.contains("searchOrganizations(queryParams?: { name?: string; \"search-limit\"?: number; }, options?: AxiosRequestConfig): RestResponse<Organization[]>;"));
    }

    @Test
    public void basicClientTest() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateJaxrsApplicationClient = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(OrganizationApplication.class));
        final String errorMessage = "Unexpected output: " + output;
        // HttpClient
        Assert.assertTrue(errorMessage, output.contains("interface HttpClient"));
        Assert.assertTrue(errorMessage, output.contains("request<R>(requestConfig: { method: string; url: string; queryParams?: any; data?: any; copyFn?: (data: R) => R; }): RestResponse<R>;"));
        // application client
        Assert.assertTrue(errorMessage, output.contains("class OrganizationApplicationClient"));
        Assert.assertTrue(errorMessage, output.contains("getPerson(personId: number): RestResponse<Person>"));
        Assert.assertTrue(errorMessage, output.contains("return this.httpClient.request({ method: \"GET\", url: uriEncoding`api/people/${personId}` });"));
        Assert.assertTrue(errorMessage, output.contains("getAddress(personId: number, addressId: number): RestResponse<Address>"));
        Assert.assertTrue(errorMessage, output.contains("return this.httpClient.request({ method: \"GET\", url: uriEncoding`api/people/${personId}/address/${addressId}` });"));
        Assert.assertTrue(errorMessage, output.contains("type RestResponse<R> = Promise<R>;"));
        // helper
        Assert.assertTrue(errorMessage, output.contains("function uriEncoding"));
    }

    @Test
    public void clientCustomizationTest() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateJaxrsApplicationClient = true;
        settings.restResponseType = "AxiosPromise";
        settings.restOptionsType = "AxiosRequestConfig";
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(OrganizationApplication.class));
        final String errorMessage = "Unexpected output: " + output;
        // HttpClient
        Assert.assertTrue(errorMessage, output.contains("request<R>(requestConfig: { method: string; url: string; queryParams?: any; data?: any; copyFn?: (data: R) => R; options?: AxiosRequestConfig; }): RestResponse<R>;"));
        // application client
        Assert.assertTrue(errorMessage, output.contains("class OrganizationApplicationClient"));
        Assert.assertTrue(errorMessage, output.contains("getPerson(personId: number, options?: AxiosRequestConfig): RestResponse<Person>"));
        Assert.assertTrue(errorMessage, output.contains("return this.httpClient.request({ method: \"GET\", url: uriEncoding`api/people/${personId}`, options: options });"));
        Assert.assertTrue(errorMessage, output.contains("type RestResponse<R> = AxiosPromise;"));
    }

    @Test
    public void testNamespacingPerResource() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateJaxrsApplicationInterface = true;
        settings.generateJaxrsApplicationClient = true;
        settings.restNamespacing = RestNamespacing.perResource;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(OrganizationApplication.class));
        final String errorMessage = "Unexpected output: " + output;
        Assert.assertTrue(errorMessage, !output.contains("class OrganizationApplicationClient"));
        Assert.assertTrue(errorMessage, output.contains("class OrganizationsResourceClient implements OrganizationsResource "));
        Assert.assertTrue(errorMessage, !output.contains("class OrganizationResourceClient"));
        Assert.assertTrue(errorMessage, output.contains("class PersonResourceClient implements PersonResource "));
    }

    @Test
    public void testNamespacingByAnnotation() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateJaxrsApplicationInterface = true;
        settings.generateJaxrsApplicationClient = true;
        settings.restNamespacing = RestNamespacing.byAnnotation;
        settings.restNamespacingAnnotation = Api.class;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(OrganizationApplication.class));
        final String errorMessage = "Unexpected output: " + output;
        Assert.assertTrue(errorMessage, output.contains("class OrgApiClient implements OrgApi "));
        Assert.assertTrue(errorMessage, output.contains("class OrganizationApplicationClient implements OrganizationApplication "));
        Assert.assertTrue(errorMessage, !output.contains("class OrganizationsResourceClient"));
        Assert.assertTrue(errorMessage, !output.contains("class OrganizationResourceClient"));
        Assert.assertTrue(errorMessage, !output.contains("class PersonResourceClient"));
    }

    @Test
    public void testJavadoc() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateJaxrsApplicationInterface = true;
        settings.javadocXmlFiles = Arrays.asList(new File("src/test/javadoc/test-javadoc.xml"));
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(OrganizationApplication.class));
        Assert.assertTrue(output.contains("Returns person with specified ID."));
    }

    @Test
    public void testSwaggerComments() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateJaxrsApplicationInterface = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(OrganizationApplication.class));
        Assert.assertTrue(output.contains("Comment in swagger annotation"));
        Assert.assertTrue(output.contains("Response code 200 - ok"));
        Assert.assertTrue(output.contains("Response code 400 - not ok"));
    }

    @Test
    public void testDeprecatedAnnotationComment() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.generateJaxrsApplicationInterface = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(OrganizationApplication.class));
        Assert.assertTrue(output.contains("@deprecated"));
    }

    @ApplicationPath("api")
    public static class OrganizationApplication extends Application {
        @Override
        public Set<Class<?>> getClasses() {
            return new LinkedHashSet<>(Arrays.asList(
                    OrganizationsResource.class,
                    PersonResource.class
            ));
        }
    }

    @Api("OrgApi")
    @Path("organizations")
    public static class OrganizationsResource {
        @PathParam("organizationId")
        protected long organizationId;
        @GET
        public List<Organization> searchOrganizations(@QueryParam("name") String oranizationName, @QueryParam("search-limit") int searchLimit) {
            return null;
        }
        @Path("{ organizationCode : [a-z]+ }/{organizationId}")
        public OrganizationResource getOrganizationResource() {
            return null;
        }
    }

    public static class OrganizationResource {
        @GET
        public Organization getOrganization() {
            return null;
        }
        @PUT
        public void setOrganization(@PathParam("organizationCode") String organizationCode, Organization organization) {
        }
    }

    public static class Organization {
        public String name;
    }

    @Path("people/{personId}")
    public static class PersonResource {

        @PathParam("personId")
        protected long personId;

        /**
         * Returns person with specified ID.
         */
        @ApiOperation(value = "Comment in swagger annotation")
        @ApiResponses({
            @ApiResponse(code = 200, message = "ok"),
            @ApiResponse(code = 400, message = "not ok"),
        })
        @GET
        public Person getPerson() {
            return null;
        }

        @GET
        @Path("address/{address-id}")
        @Deprecated
        public Address getAddress(@PathParam("address-id") long addressId) {
            return null;
        }
    }

    public static class Person {
        public String name;

        public Person(String name) {
            this.name = name;
        }
    }

    public static class Address {
        public String name;
    }

    @Path("conflict")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public static class NameConflictResource {
        @POST
        public Person person(Person person) {
            return new Person("POST");
        }
        @GET
        public Person person() {
            return new Person("A");
        }
        @GET
        @Path("search")
        public Person person(@QueryParam("search") String search) {
            return new Person("B");
        }
        @GET
        @Path("{person-id:.+}")
        public Person person(@PathParam("person-id") long personId) {
            return new Person("C");
        }
    }

    @Test
    public void testGettingValidIdentifierName() {
        Assert.assertEquals("foo", ModelCompiler.getValidIdentifierName("foo"));
        Assert.assertEquals("personId", ModelCompiler.getValidIdentifierName("person-id"));
        Assert.assertEquals("veryLongParameterName", ModelCompiler.getValidIdentifierName("very-long-parameter-name"));
        Assert.assertEquals("$nameWithDollar", ModelCompiler.getValidIdentifierName("$nameWithDollar"));
        Assert.assertEquals("NameWithManyDashes", ModelCompiler.getValidIdentifierName("-name--with-many---dashes-"));
        Assert.assertEquals("a2b3c4", ModelCompiler.getValidIdentifierName("1a2b3c4"));
        Assert.assertEquals("a2b3c4", ModelCompiler.getValidIdentifierName("111a2b3c4"));
    }

    @Test
    public void testEnumQueryParam() {
        final Settings settings = TestUtils.settings();
        settings.generateJaxrsApplicationInterface = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(EnumQueryParamResource.class));
        Assert.assertTrue(output.contains("queryParams?: { target?: TargetEnum; }"));
        Assert.assertTrue(output.contains("type TargetEnum = \"Target1\" | \"Target2\""));
    }

    @Path("enum-query-param")
    public static class EnumQueryParamResource {
        @GET
        @Path("somePath")
        public List<String> getFoo(@QueryParam("target") TargetEnum target) {
            return Collections.emptyList();
        }
    }

    public enum TargetEnum {
        Target1, Target2
    }

    @Test
    public void testBeanParam() {
        final Settings settings = TestUtils.settings();
        settings.generateJaxrsApplicationInterface = true;
        settings.generateJaxrsApplicationClient = true;
        settings.outputFileType = TypeScriptFileType.implementationFile;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(BeanParamResource.class));
        Assert.assertTrue(output.contains("interface SearchParams1QueryParams"));
        Assert.assertTrue(output.contains("interface SearchParams2QueryParams"));
        Assert.assertTrue(output.contains("queryParams?: SearchParams1QueryParams & SearchParams2QueryParams & { message?: string; }"));
    }

    public static class SearchParams1 {
        @QueryParam("id")
        private Integer id;

        @QueryParam("name")
        private String name;
    }

    public static class SearchParams2 {
        private String description;
        @QueryParam("description")
        public void setDescription(String description) {
            this.description = description;
        }
    }

    @Test
    public void testPathParameterWithReservedWord() {
        final Settings settings = TestUtils.settings();
        settings.generateJaxrsApplicationInterface = true;
        settings.generateJaxrsApplicationClient = true;
        settings.outputFileType = TypeScriptFileType.implementationFile;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ResourceWithReservedWord.class));
        Assert.assertTrue(output.contains("getLogs(_class: string): RestResponse<string[]>;"));
        Assert.assertTrue(output.contains("getLogs(_class: string): RestResponse<string[]> {"));
        Assert.assertTrue(output.contains("uriEncoding`logs/${_class}`"));
    }

    @Path("")
    public static class ResourceWithReservedWord {

        @GET
        @Path("/logs/{class}")
        public Collection<String> getLogs(@PathParam("class") String clazz) {
            return null;
        }
    }

//    http://localhost:9998/bean-param?id=1&name=vh&description=desc&message=hello

    @Path("bean-param")
    @Produces(MediaType.APPLICATION_JSON)
    public static class BeanParamResource {

        @GET
        public List<String> getItems(
                @BeanParam SearchParams1 params1,
                @BeanParam SearchParams2 params2,
                @QueryParam("message") String message
        ) {
            return Collections.emptyList();
        }
    }

    @Test
    public void testRegExpInPath() {
        final Settings settings = TestUtils.settings();
        settings.generateJaxrsApplicationClient = true;
        settings.outputFileType = TypeScriptFileType.implementationFile;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(RegExpResource.class));
        Assert.assertTrue(output.contains("getWithId(id: number)"));
        Assert.assertTrue(output.contains("url: uriEncoding`objects/${id}`"));
    }

    @Path("objects")
    public static class RegExpResource {
        @GET
        @Path("{id: [0-9]{1,99}}")
//        @Path("{id: [0-9]+}")
        public String getWithId(@PathParam("id") long id) {
            return null;
        }
    }

    @Test
    public void testGenericResources() {
        final Settings settings = TestUtils.settings();
        settings.generateJaxrsApplicationClient = true;
        settings.outputFileType = TypeScriptFileType.implementationFile;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(AccountResource.class));
        Assert.assertTrue(!output.contains("get(id: ID): RestResponse<ENTITY>"));
        Assert.assertTrue(output.contains("get(id: number): RestResponse<AccountDto>"));
        Assert.assertTrue(output.contains("interface AccountDto"));
    }

    public static class AccountDto {
        public Integer id;
        public String name;
    }

    public static interface AbstractCrudResource<ENTITY, ID> {
        @GET
        @Path("{id}")
        public ENTITY get(@PathParam("id") ID id);
    }

    @Path("/account")
    public static interface AccountResource extends AbstractCrudResource<AccountDto, Integer> {
        @GET
        @Path("/test")
        void test();
    }

    public static void main(String[] args) {
        final ResourceConfig config = new ResourceConfig(BeanParamResource.class, JacksonFeature.class);
        JdkHttpServerFactory.createHttpServer(URI.create("http://localhost:9998/"), config);
        System.out.println("Jersey started.");
    }

}
