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
                """
                class Abstract<T> {
                    dataFromAbstract?: T;
                    nonNullableBooleanFromAbstract: boolean;
                    nonNullableFlagFromAbstract: boolean;
                    nonNullableStringFromAbstract: string;
                    nullableArrayFromAbstract?: (string | undefined)[];
                    nullableBooleanFromAbstract?: boolean;
                    nullableFlagFromAbstract?: boolean;
                    nullableGenericArrayFromAbstract?: (T | undefined)[];
                    nullableGenericTypeFromAbstract?: T;
                    nullableListFromAbstract?: (string | undefined)[];
                    nullableStringFromAbstract?: string;
                    testFromAbstract: string;
                    testNullableFromAbstract?: string;
                }
                
                class A<T> extends Abstract<T> implements Interface<T> {
                    data?: T;
                    dataFromInterface?: T;
                    nonNullableBoolean: boolean;
                    nonNullableBooleanFromInterface: boolean;
                    nonNullableFlag: boolean;
                    nonNullableFlagFromInterface: boolean;
                    nonNullableString: string;
                    nonNullableStringFromInterface: string;
                    nullableArray?: (string | undefined)[];
                    nullableArrayFromInterface?: (string | undefined)[];
                    nullableBoolean?: boolean;
                    nullableBooleanFromInterface?: boolean;
                    nullableFlag?: boolean;
                    nullableFlagFromInterface?: boolean;
                    nullableGenericArray?: (T | undefined)[];
                    nullableGenericArrayFromInterface?: (T | undefined)[];
                    nullableGenericType?: T;
                    nullableGenericTypeFromInterface?: T;
                    nullableList?: (string | undefined)[];
                    nullableListFromInterface?: (string | undefined)[];
                    nullableString?: string;
                    nullableStringFromInterface?: string;
                    test: string;
                    testFromInterface: string;
                    testNullable?: string;
                    testNullableFromInterface?: string;
                }
                
                interface Interface<T> {
                    dataFromInterface?: T;
                    nonNullableBooleanFromInterface: boolean;
                    nonNullableFlagFromInterface: boolean;
                    nonNullableStringFromInterface: string;
                    nullableArrayFromInterface?: (string | undefined)[];
                    nullableBooleanFromInterface?: boolean;
                    nullableFlagFromInterface?: boolean;
                    nullableGenericArrayFromInterface?: (T | undefined)[];
                    nullableGenericTypeFromInterface?: T;
                    nullableListFromInterface?: (string | undefined)[];
                    nullableStringFromInterface?: string;
                    testFromInterface: string;
                    testNullableFromInterface?: string;
                }""".trimIndent()
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