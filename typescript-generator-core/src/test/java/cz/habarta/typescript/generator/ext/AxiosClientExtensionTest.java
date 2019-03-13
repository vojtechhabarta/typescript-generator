
package cz.habarta.typescript.generator.ext;

import cz.habarta.typescript.generator.Input;
import cz.habarta.typescript.generator.JaxrsApplicationTest;
import cz.habarta.typescript.generator.RestNamespacing;
import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.TestUtils;
import cz.habarta.typescript.generator.TypeScriptFileType;
import cz.habarta.typescript.generator.TypeScriptGenerator;
import cz.habarta.typescript.generator.TypeScriptOutputKind;
import org.junit.Assert;
import org.junit.Test;


public class AxiosClientExtensionTest {

    @Test
    public void test() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.outputKind = TypeScriptOutputKind.module;
        settings.generateJaxrsApplicationClient = true;
        settings.jaxrsNamespacing = RestNamespacing.perResource;
        settings.extensions.add(new AxiosClientExtension());
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(JaxrsApplicationTest.OrganizationApplication.class));
        final String errorMessage = "Unexpected output: " + output;

        Assert.assertTrue(errorMessage, output.contains("interface Organization"));
        Assert.assertTrue(errorMessage, output.contains("interface Address"));
        Assert.assertTrue(errorMessage, output.contains("interface Person"));
        Assert.assertTrue(errorMessage, output.contains("interface HttpClient"));

        Assert.assertTrue(errorMessage, output.contains("class OrganizationsResourceClient<O>"));
        Assert.assertTrue(errorMessage, output.contains("class PersonResourceClient<O>"));
        Assert.assertTrue(errorMessage, output.contains("type RestResponse<R> = Promise<Axios.GenericAxiosResponse<R>>"));

        Assert.assertTrue(errorMessage, output.contains("class AxiosHttpClient implements HttpClient<Axios.AxiosRequestConfig>"));
        Assert.assertTrue(errorMessage, output.contains("request<R>(requestConfig: { method: string; url: string; queryParams?: any; data?: any; copyFn?: (data: R) => R; options?: Axios.AxiosRequestConfig; }): RestResponse<R>"));
        Assert.assertTrue(errorMessage, output.contains("class AxiosOrganizationsResourceClient extends OrganizationsResourceClient<Axios.AxiosRequestConfig>"));
        Assert.assertTrue(errorMessage, output.contains("class AxiosPersonResourceClient extends PersonResourceClient<Axios.AxiosRequestConfig>"));
        Assert.assertTrue(errorMessage, output.contains("constructor(baseURL: string, axiosInstance: Axios.AxiosInstance = axios.create())"));
    }

}
