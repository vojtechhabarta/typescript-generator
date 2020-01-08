package cz.habarta.typescript.generator.spring

import cz.habarta.typescript.generator.Input
import cz.habarta.typescript.generator.NullabilityDefinition
import cz.habarta.typescript.generator.TestUtils
import cz.habarta.typescript.generator.TypeScriptFileType
import cz.habarta.typescript.generator.TypeScriptGenerator
import org.junit.Assert
import org.junit.Test
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

class SpringKotlinTest {

    @Test
    fun testSpring() {
        val settings = TestUtils.settings()
        settings.outputFileType = TypeScriptFileType.implementationFile
        settings.generateSpringApplicationClient = true
        settings.nullabilityDefinition = NullabilityDefinition.undefinedInlineUnion
        val output = TypeScriptGenerator(settings).generateTypeScript(Input.from(B::class.java))
        val errorMessage = "Unexpected output: $output"
        Assert.assertTrue(errorMessage, output.contains("doSomething(arg0?: (A<string> | undefined)[]): RestResponse<(string | undefined)[] | undefined>"))
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

    @RestController
    @RequestMapping("")
    private class B {

        @PostMapping
        fun doSomething(@RequestBody body: List<A<String>?>?): List<String?>? {
            return null
        }
    }

}
