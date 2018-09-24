
package cz.habarta.typescript.generator;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import org.junit.Assert;
import org.junit.Test;


public class SwaggerTest {

    @Test
    public void test() {
        final Settings settings = TestUtils.settings();
        settings.generateJaxrsApplicationInterface = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(TestApplication.class));
        Assert.assertTrue(output.contains("interface TestResponse"));
        Assert.assertTrue(output.contains("interface TestError"));
        Assert.assertTrue(output.contains("testOperationError(): RestResponse<any>;"));
        Assert.assertTrue(output.contains("testOperation1(): RestResponse<TestResponse>;"));
        Assert.assertTrue(output.contains("testOperation2(): RestResponse<TestResponse[]>;"));
        Assert.assertTrue(output.contains("testOperation3(): RestResponse<TestResponse[]>;"));
        Assert.assertTrue(output.contains("testOperation4(): RestResponse<{ [index: string]: TestResponse }>;"));
        Assert.assertTrue(!output.contains("testHiddenOperation"));
    }

    @Test
    public void testDocumentation() {
        final Settings settings = TestUtils.settings();
        settings.generateJaxrsApplicationInterface = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(DocumentedApplication.class));
        Assert.assertTrue(output.contains("Documentation for operation 1."));
        Assert.assertTrue(output.contains("Bad Request"));
        Assert.assertTrue(output.contains("Not Found"));
        Assert.assertTrue(output.contains("Documentation for the bean."));
        Assert.assertTrue(output.contains("Documentation for property 1."));
        Assert.assertTrue(output.contains("Documentation for property 2."));
        Assert.assertTrue(output.contains("Documentation for property 3."));
    }

    @Test
    public void testDataType() {
        final Settings settings = TestUtils.settings();
        settings.generateJaxrsApplicationInterface = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(DocumentedApplication.class));
        Assert.assertTrue(output.contains("property1: string"));
        Assert.assertTrue(output.contains("property2?: string"));
        Assert.assertTrue(output.contains("property3: string"));
    }

    @Test
    public void testSwaggerOff() {
        final Settings settings = TestUtils.settings();
        settings.generateJaxrsApplicationInterface = true;
        settings.ignoreSwaggerAnnotations = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(DocumentedApplication.class));
        Assert.assertTrue(!output.contains("Documentation for operation 1."));
        Assert.assertTrue(!output.contains("Bad Request"));
        Assert.assertTrue(!output.contains("Not Found"));
        Assert.assertTrue(!output.contains("Documentation for the bean."));
        Assert.assertTrue(!output.contains("Documentation for property 1."));
        Assert.assertTrue(!output.contains("Documentation for property 2."));
        Assert.assertTrue(!output.contains("Documentation for property 3."));
        Assert.assertTrue(output.contains("property1: string"));
        Assert.assertTrue(output.contains("property2: number"));
        Assert.assertTrue(output.contains("property3: number"));
    }

    private static class TestApplication extends Application {
        @Override
        public Set<Class<?>> getClasses() {
            return new LinkedHashSet<>(Arrays.<Class<?>>asList(TestResource.class));
        }
    }

    @Path("test")
    private static class TestResource {

        @ApiOperation(value = "", response = TestResponse.class)
        @GET
        public Response testOperation1() {
            return Response.ok(new TestResponse()).build();
        }

        @ApiOperation(value = "", responseContainer = "List", response = TestResponse.class)
        @GET
        public Response testOperation2() {
            return Response.ok(new TestResponse()).build();
        }

        @ApiOperation(value = "", responseContainer = "Set", response = TestResponse.class)
        @GET
        public Response testOperation3() {
            return Response.ok(new TestResponse()).build();
        }

        @ApiOperation(value = "", responseContainer = "Map", response = TestResponse.class)
        @GET
        public Response testOperation4() {
            return Response.ok(new TestResponse()).build();
        }

        @ApiResponses({@ApiResponse(code = 400, message = "", response = TestError.class)})
        @GET
        public Response testOperationError() {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        @ApiOperation(value = "", hidden = true)
        @GET
        public Response testHiddenOperation() {
            return null;
        }

    }

    private static class TestResponse {
    }

    private static class TestError {
    }


    private static class DocumentedApplication extends Application {
        @Override
        public Set<Class<?>> getClasses() {
            return new LinkedHashSet<>(Arrays.<Class<?>>asList(DocumentedResource.class));
        }
    }

    @Path("test")
    private static class DocumentedResource {

        @ApiOperation("Documentation for operation 1.")
        @ApiResponses({
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 404, message = "Not Found"),
        })
        @GET
        public DocumentedBean documentedOperation1() {
            return null;
        }

    }

    @ApiModel(description = "Documentation for the bean.")
    private static class DocumentedBean {

        @ApiModelProperty("Documentation for property 1.")
        public String property1;

        /**
         * Sometimes custom serializers are used. In such cases target type is overridden explicitly
         * with dataType annotation attribute.
         */
        @ApiModelProperty(value = "Documentation for property 2.", dataType = "java.lang.String")
        public Long property2;

        @ApiModelProperty(value = "Documentation for property 3.", dataType = "java.lang.String", required = true)
        public Long property3;
    }

}
