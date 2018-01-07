
package cz.habarta.typescript.generator.sample

data class PersonKt (
    val name: String,
    val age: Int,
    val hasChildren: Boolean,
    val tags: List<String>,
    val emails: Map<String, String>
)
