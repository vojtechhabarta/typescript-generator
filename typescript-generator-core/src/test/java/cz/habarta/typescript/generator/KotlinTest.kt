package cz.habarta.typescript.generator

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import javax.ws.rs.POST
import javax.ws.rs.Path

@Suppress("UNUSED_PARAMETER")
class KotlinTest {

    @Test
    fun testJaxRS() {
        val settings = TestUtils.settings()
        settings.generateJaxrsApplicationInterface = true
        settings.nullabilityDefinition = NullabilityDefinition.undefinedInlineUnion
        val output = TypeScriptGenerator(settings).generateTypeScript(Input.from(B::class.java))
        val errorMessage = "Unexpected output: $output"
        Assertions.assertTrue(output.contains("doSomething(arg0?: (A<string> | undefined)[]): RestResponse<(string | undefined)[] | undefined>;"), errorMessage)
    }

    @Test
    fun testClassBody() {
        testOutput(A::class.java,
                "class A<T> {\n" +
                        "    data?: T;\n" +
                        "    nonNullableBoolean: boolean;\n" +
                        "    nonNullableFlag: boolean;\n" +
                        "    nonNullableString: string;\n" +
                        "    nullableArray?: (string | undefined)[];\n" +
                        "    nullableBoolean?: boolean;\n" +
                        "    nullableFlag?: boolean;\n" +
                        "    nullableGenericArray?: (T | undefined)[];\n" +
                        "    nullableGenericType?: T;\n" +
                        "    nullableList?: (string | undefined)[];\n" +
                        "    nullableString?: string;\n" +
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
        val nullableBoolean: Boolean? = null
        val nonNullableBoolean: Boolean = false
        val isNullableFlag: Boolean? = false
        val isNonNullableFlag: Boolean = false

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
        settings.nullabilityDefinition = NullabilityDefinition.undefinedInlineUnion
        settings.sortDeclarations = true
        val output = TypeScriptGenerator(settings).generateTypeScript(Input.from(inputClass))
        Assertions.assertEquals(expected.replace('\'', '"'), output.trim { it <= ' ' })
    }

    @Test
    fun testEnumTypeVariableBound() {
        val settings = TestUtils.settings()
        val output = TypeScriptGenerator(settings).generateTypeScript(Input.from(A2::class.java))
        val errorMessage = "Unexpected output: $output"
        Assertions.assertTrue(output.contains("interface A2<S>"), errorMessage)
    }

    private class A2<S> where S : Enum<S> {
        fun getData2(): S? {
            return null
        }
    }

}