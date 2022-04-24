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
                "class Abstract<T> {\n" +
                        "    dataFromAbstract?: T;\n" +
                        "    nonNullableBooleanFromAbstract: boolean;\n" +
                        "    nonNullableFlagFromAbstract: boolean;\n" +
                        "    nonNullableStringFromAbstract: string;\n" +
                        "    nullableArrayFromAbstract?: (string | undefined)[];\n" +
                        "    nullableBooleanFromAbstract?: boolean;\n" +
                        "    nullableFlagFromAbstract?: boolean;\n" +
                        "    nullableGenericArrayFromAbstract?: (T | undefined)[];\n" +
                        "    nullableGenericTypeFromAbstract?: T;\n" +
                        "    nullableListFromAbstract?: (string | undefined)[];\n" +
                        "    nullableStringFromAbstract?: string;\n" +
                        "    testFromAbstract: string;\n" +
                        "    testNullableFromAbstract?: string;\n" +
                        "}\n" +
                        "\n" +
                        "class A<T> extends Abstract<T> implements Interface<T> {\n" +
                        "    data?: T;\n" +
                        "    dataFromInterface?: T;\n" +
                        "    nonNullableBoolean: boolean;\n" +
                        "    nonNullableBooleanFromInterface: boolean;\n" +
                        "    nonNullableFlag: boolean;\n" +
                        "    nonNullableFlagFromInterface: boolean;\n" +
                        "    nonNullableString: string;\n" +
                        "    nonNullableStringFromInterface: string;\n" +
                        "    nullableArray?: (string | undefined)[];\n" +
                        "    nullableArrayFromInterface?: (string | undefined)[];\n" +
                        "    nullableBoolean?: boolean;\n" +
                        "    nullableBooleanFromInterface?: boolean;\n" +
                        "    nullableFlag?: boolean;\n" +
                        "    nullableFlagFromInterface?: boolean;\n" +
                        "    nullableGenericArray?: (T | undefined)[];\n" +
                        "    nullableGenericArrayFromInterface?: (T | undefined)[];\n" +
                        "    nullableGenericType?: T;\n" +
                        "    nullableGenericTypeFromInterface?: T;\n" +
                        "    nullableList?: (string | undefined)[];\n" +
                        "    nullableListFromInterface?: (string | undefined)[];\n" +
                        "    nullableString?: string;\n" +
                        "    nullableStringFromInterface?: string;\n" +
                        "    test: string;\n" +
                        "    testFromInterface: string;\n" +
                        "    testNullable?: string;\n" +
                        "    testNullableFromInterface?: string;\n" +
                        "}\n" +
                        "\n" +
                        "interface Interface<T> {\n" +
                        "    dataFromInterface?: T;\n" +
                        "    nonNullableBooleanFromInterface: boolean;\n" +
                        "    nonNullableFlagFromInterface: boolean;\n" +
                        "    nonNullableStringFromInterface: string;\n" +
                        "    nullableArrayFromInterface?: (string | undefined)[];\n" +
                        "    nullableBooleanFromInterface?: boolean;\n" +
                        "    nullableFlagFromInterface?: boolean;\n" +
                        "    nullableGenericArrayFromInterface?: (T | undefined)[];\n" +
                        "    nullableGenericTypeFromInterface?: T;\n" +
                        "    nullableListFromInterface?: (string | undefined)[];\n" +
                        "    nullableStringFromInterface?: string;\n" +
                        "    testFromInterface: string;\n" +
                        "    testNullableFromInterface?: string;\n" +
                        "}"
        )
    }

    private interface Interface<T> {
        val nullableStringFromInterface: String?
        val nonNullableStringFromInterface: String
        val nullableListFromInterface: List<String?>?
        val nullableArrayFromInterface: Array<String?>?
        val nullableGenericArrayFromInterface: Array<T?>?
        val nullableGenericTypeFromInterface: T?
        val nullableBooleanFromInterface: Boolean?
        val nonNullableBooleanFromInterface: Boolean
        val isNullableFlagFromInterface: Boolean?
        val isNonNullableFlagFromInterface: Boolean
        fun <B: T> getDataFromInterface(): B?
        fun getTestFromInterface(): String
        fun getTestNullableFromInterface(): String?
    }

    private abstract class Abstract<T> {
        abstract val nullableStringFromAbstract: String?
        abstract val nonNullableStringFromAbstract: String
        abstract val nullableListFromAbstract: List<String?>?
        abstract val nullableArrayFromAbstract: Array<String?>?
        abstract val nullableGenericArrayFromAbstract: Array<T?>?
        abstract val nullableGenericTypeFromAbstract: T?
        abstract val nullableBooleanFromAbstract: Boolean?
        abstract val nonNullableBooleanFromAbstract: Boolean
        abstract val isNullableFlagFromAbstract: Boolean?
        abstract val isNonNullableFlagFromAbstract: Boolean
        abstract fun <B: T> getDataFromAbstract(): B?
        abstract fun getTestFromAbstract(): String
        abstract fun getTestNullableFromAbstract(): String?
    }

    private class A<T> : Abstract<T>(), Interface<T> {
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
        fun <B: T> getData(): B? = null
        fun getTest(): String = ""
        fun getTestNullable(): String? = ""

        override val nullableStringFromAbstract: String? = null
        override val nonNullableStringFromAbstract: String = ""
        override val nullableListFromAbstract: List<String?>? = null
        override val nullableArrayFromAbstract: Array<String?>? = null
        override val nullableGenericArrayFromAbstract: Array<T?>? = null
        override val nullableGenericTypeFromAbstract: T? = null
        override val nullableBooleanFromAbstract: Boolean? = null
        override val nonNullableBooleanFromAbstract: Boolean = false
        override val isNullableFlagFromAbstract: Boolean? = false
        override val isNonNullableFlagFromAbstract: Boolean = false
        override fun <B: T> getDataFromAbstract(): B? = null
        override fun getTestFromAbstract(): String = ""
        override fun getTestNullableFromAbstract(): String? = ""

        override val nullableStringFromInterface: String? = null
        override val nonNullableStringFromInterface: String = ""
        override val nullableListFromInterface: List<String?>? = null
        override val nullableArrayFromInterface: Array<String?>? = null
        override val nullableGenericArrayFromInterface: Array<T?>? = null
        override val nullableGenericTypeFromInterface: T? = null
        override val nullableBooleanFromInterface: Boolean? = null
        override val nonNullableBooleanFromInterface: Boolean = false
        override val isNullableFlagFromInterface: Boolean? = false
        override val isNonNullableFlagFromInterface: Boolean = false
        override fun <B: T> getDataFromInterface(): B? = null
        override fun getTestFromInterface(): String = ""
        override fun getTestNullableFromInterface(): String? = ""
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