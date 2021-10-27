
package cz.habarta.typescript.generator;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class Swagger3Test {

    @Test
    public void test() {
        final Settings settings = TestUtils.settings();
        settings.generateJaxrsApplicationInterface = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(TestApplication.class));
        Assertions.assertTrue(output.contains("interface TestResponse"));
        Assertions.assertTrue(output.contains("interface TestError"));
        Assertions.assertTrue(output.contains("testOperationError(): RestResponse<any>;"));
        Assertions.assertTrue(output.contains("testOperation1a(): RestResponse<TestResponse>;"));
        Assertions.assertTrue(output.contains("testOperation1b(): RestResponse<TestResponse>;"));
        Assertions.assertTrue(output.contains("testOperation1c(): RestResponse<TestResponse>;"));
//        Assertions.assertTrue(output.contains("testOperation2(): RestResponse<TestResponse[]>;"));
//        Assertions.assertTrue(output.contains("testOperation3(): RestResponse<TestResponse[]>;"));
//        Assertions.assertTrue(output.contains("testOperation4(): RestResponse<{ [index: string]: TestResponse }>;"));
        Assertions.assertTrue(!output.contains("testHiddenOperation"));
    }

    @Test
    public void testDocumentation() {
        final Settings settings = TestUtils.settings();
        settings.generateJaxrsApplicationInterface = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(DocumentedApplication.class));
        Assertions.assertTrue(output.contains("Documentation for operation 1."));
        Assertions.assertTrue(output.contains("Bad Request"));
        Assertions.assertTrue(output.contains("Not Found"));
        Assertions.assertTrue(output.contains("Documentation for the bean."));
        Assertions.assertTrue(output.contains("Documentation for property 1."));
        Assertions.assertTrue(output.contains("Documentation for property 2."));
        Assertions.assertTrue(output.contains("Documentation for property 3."));
    }

    @Test
    public void testDataType() {
        final Settings settings = TestUtils.settings();
        settings.generateJaxrsApplicationInterface = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(DocumentedApplication.class));
        Assertions.assertTrue(output.contains("property1: string"));
        Assertions.assertTrue(output.contains("property2?: string"));
        Assertions.assertTrue(output.contains("property3: string"));
    }

    @Test
    public void testSwaggerOff() {
        final Settings settings = TestUtils.settings();
        settings.generateJaxrsApplicationInterface = true;
        settings.ignoreSwaggerAnnotations = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(DocumentedApplication.class));
        Assertions.assertTrue(!output.contains("Documentation for operation 1."));
        Assertions.assertTrue(!output.contains("Bad Request"));
        Assertions.assertTrue(!output.contains("Not Found"));
        Assertions.assertTrue(!output.contains("Documentation for the bean."));
        Assertions.assertTrue(!output.contains("Documentation for property 1."));
        Assertions.assertTrue(!output.contains("Documentation for property 2."));
        Assertions.assertTrue(!output.contains("Documentation for property 3."));
        Assertions.assertTrue(output.contains("property1: string"));
        Assertions.assertTrue(output.contains("property2: number"));
        Assertions.assertTrue(output.contains("property3: number"));
    }

    private static class TestApplication extends Application {
        @Override
        public Set<Class<?>> getClasses() {
            return new LinkedHashSet<>(Arrays.<Class<?>>asList(TestResource.class));
        }
    }

    @Path("test")
    private static class TestResource {

        @Operation(responses = @ApiResponse(content = @Content(schema = @Schema(implementation = TestResponse.class))))
        @GET
        public Response testOperation1a() {
            return Response.ok(new TestResponse()).build();
        }

        @ApiResponse(content = @Content(schema = @Schema(implementation = TestResponse.class)))
        @GET
        public Response testOperation1b() {
            return Response.ok(new TestResponse()).build();
        }

        @ApiResponses(@ApiResponse(content = @Content(schema = @Schema(implementation = TestResponse.class))))
        @GET
        public Response testOperation1c() {
            return Response.ok(new TestResponse()).build();
        }

//        @Operation(responses = @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = TestResponse.class)))))
//        @GET
//        public Response testOperation2() {
//            return Response.ok(new TestResponse()).build();
//        }
//
//        @Operation(responses = @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = TestResponse.class)))))
//        @GET
//        public Response testOperation3() {
//            return Response.ok(new TestResponse()).build();
//        }
//
//        @Operation(responseContainer = "Map", response = TestResponse.class)
//        @GET
//        public Response testOperation4() {
//            return Response.ok(new TestResponse()).build();
//        }

        @ApiResponses({@ApiResponse(responseCode = "400", content = {@Content(schema = @Schema(implementation = TestError.class))})})
        @GET
        public Response testOperationError() {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        @Operation(hidden = true)
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

        @Operation(description = "Documentation for operation 1.")
        @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
        })
        @GET
        public DocumentedBean documentedOperation1() {
            return null;
        }

    }

    @Schema(description = "Documentation for the bean.")
    private static class DocumentedBean {

        @Schema(description = "Documentation for property 1.")
        public String property1;

        /**
         * Sometimes custom serializers are used. In such cases target type is overridden explicitly
         * with dataType annotation attribute.
         */
        @Schema(description = "Documentation for property 2.", implementation = String.class)
        public Long property2;

        @Schema(description = "Documentation for property 3.", implementation = String.class, required = true)
        public Long property3;
    }

}
