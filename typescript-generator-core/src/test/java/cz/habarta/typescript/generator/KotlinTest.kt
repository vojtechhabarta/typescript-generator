package cz.habarta.typescript.generator

import org.junit.Assert
import org.junit.Test
import javax.ws.rs.POST
import javax.ws.rs.Path

class KotlinTest {

    @Test
    fun testJaxRS() {
        val settings = TestUtils.settings()
        settings.generateJaxrsApplicationInterface = true
        val output = TypeScriptGenerator(settings).generateTypeScript(Input.from(B::class.java))
        val errorMessage = "Unexpected output: $output"
        Assert.assertTrue(errorMessage, output.contains("doSomething(arg0?: (A<string> | undefined)[]): RestResponse<(string | undefined)[] | undefined>;"))
    }

    @Test
    fun testClassBody() {
        testOutput(A::class.java,
                        "class A<T> {\n" +
                        "    nullableString?: string;\n" +
                        "    nonNullableString: string;\n" +
                        "    nullableList?: (string | undefined)[];\n" +
                        "    nullableArray?: (string | undefined)[];\n" +
                        "    nullableGenericArray?: (T | undefined)[];\n" +
                        "    nullableGenericType?: T;\n" +
                        "    data?: T;\n" +
                        "    test: string;\n" +
                        "    testNullable?: string;\n" +
                        "}"
        )
    }

    private class A<T> {
        val nullableString: String? = null
        val nonNullableString: String = ""
        val nullableList: List<String?>? = null
        val nullableArray: Array<String?>? = null
        val nullableGenericArray: Array<T?>? = null
        val nullableGenericType: T? = null

        fun <B: T> getData(): B? {
            return null;
        }

        fun getTest(): String {
            return ""
        }

        fun getTestNullable(): String? {
            return ""
        }
    }

    @Path("")
    private class B {

        @POST
        fun doSomething(body: List<A<String>?>?): List<String?>? {
            return null
        }
    }

    private fun testOutput(inputClass: Class<*>, expected: String) {
        val settings = TestUtils.settings()
        settings.jackson2Configuration = Jackson2ConfigurationResolved()
        settings.outputFileType = TypeScriptFileType.implementationFile
        settings.mapClasses = ClassMapping.asClasses
        val output = TypeScriptGenerator(settings).generateTypeScript(Input.from(inputClass))
        println(output)
        Assert.assertEquals(expected.replace('\'', '"'), output.trim { it <= ' ' })
    }
}
