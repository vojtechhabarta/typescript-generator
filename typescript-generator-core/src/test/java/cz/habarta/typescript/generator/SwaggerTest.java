
package cz.habarta.typescript.generator;

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

}
