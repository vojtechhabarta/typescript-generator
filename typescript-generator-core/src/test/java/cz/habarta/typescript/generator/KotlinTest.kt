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
        Assert.assertTrue(errorMessage, output.contains("doSomething(body?: (A | null)[]>): RestResponse<(string | null)[]>;"))
    }

    @Test
    fun testClassBody() {
        testOutput(A::class.java,
                        "class A {\n" +
                        "    nullableString?: string;\n" +
                        "    nonNullableString: string;\n" +
                        "    nullableList?: (string | null)[];\n" +
                        "    test: string;\n" +
                        "    testNullable?: string;\n" +
                        "}"
        )
    }

    private class A {
        val nullableString: String? = null
        val nonNullableString: String = ""
        val nullableList: List<String?>? = null

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
        fun doSomething(body: List<A?>?): List<String?>? {
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
