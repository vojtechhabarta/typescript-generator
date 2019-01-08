[![Maven Central](https://img.shields.io/maven-central/v/cz.habarta.typescript-generator/typescript-generator-core.svg)](https://repo1.maven.org/maven2/cz/habarta/typescript-generator/typescript-generator-core/)
[![Appveyor](https://img.shields.io/appveyor/ci/vojtechhabarta/typescript-generator/master.svg)](https://ci.appveyor.com/project/vojtechhabarta/typescript-generator)
[![Stars](https://img.shields.io/github/stars/vojtechhabarta/typescript-generator.svg?style=social)](https://github.com/vojtechhabarta/typescript-generator)

Quick links:
[Configuration parameters](http://www.habarta.cz/typescript-generator/maven/typescript-generator-maven-plugin/generate-mojo.html)
|
[Breaking changes](https://github.com/vojtechhabarta/typescript-generator/wiki/Breaking-Changes)
|
[Release notes](https://github.com/vojtechhabarta/typescript-generator/releases)
|
[Playground _(beta)_](https://jechlin.github.io/ts-gen-aws/)

typescript-generator
====================
typescript-generator is a tool for generating TypeScript definition files (.d.ts) from Java JSON classes.
If you have REST service written in Java using object to JSON mapping you can use typescript-generator to generate TypeScript interfaces from Java classes.

For example for this Java class:

``` java
public class Person {
    public String name;
    public int age;
    public boolean hasChildren;
    public List<String> tags;
    public Map<String, String> emails;
}
```

typescript-generator outputs this TypeScript interface:
``` typescript
interface Person {
    name: string;
    age: number;
    hasChildren: boolean;
    tags: string[];
    emails: { [index: string]: string };
}
```

Supported types include:
- all Java primitive types with their corresponding wrappers (for example `int` and `Integer`, `boolean` and `Boolean`, etc.)
- `String`
- `Date`
- enum
- array
- `List` and `Map` (including derived interfaces and implementation classes)
- customized type mapping

For more details see [Type Mapping Wiki page](../../wiki/Type-Mapping).

> Note: typescript-generator works with compiled classes using Java reflection. It doesn't use source files (except for Javadoc feature).
In Maven plugin this means either classes compiled from source files in the same module or classes added using `<dependency>` element.

Maven
-----

In Maven build you can use `typescript-generator-maven-plugin` like this:
``` xml
<plugin>
    <groupId>cz.habarta.typescript-generator</groupId>
    <artifactId>typescript-generator-maven-plugin</artifactId>
    <version>x.y.z</version>
    <executions>
        <execution>
            <id>generate</id>
            <goals>
                <goal>generate</goal>
            </goals>
            <phase>process-classes</phase>
        </execution>
    </executions>
    <configuration>
        <jsonLibrary>jackson2</jsonLibrary>
        <classes>
            <class>cz.habarta.typescript.generator.Person</class>
        </classes>
        <outputKind>module</outputKind>
    </configuration>
</plugin>
```

More complete sample can be found [here](sample-maven).
Detailed description how to configure typescript-generator-maven-plugin is on generated [site](http://vojtechhabarta.github.io/typescript-generator/maven/typescript-generator-maven-plugin/generate-mojo.html).


Gradle
------

In Gradle build you can use `cz.habarta.typescript-generator` plugin like this:
```groovy
apply plugin: 'cz.habarta.typescript-generator'
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath group: 'cz.habarta.typescript-generator', name: 'typescript-generator-gradle-plugin', version: 'x.y.z'
    }
}
generateTypeScript {
    jsonLibrary = 'jackson2'
    classes = [
        'cz.habarta.typescript.generator.sample.Person'
    ]
    outputFile = 'build/sample.d.ts'
    outputKind = 'global'
    namespace = 'Rest';
}
```

You can run typescript-generator on demand using `gradle generateTypeScript` command
or you can invoke it as part of another task by adding dependency from that task to `generateTypeScript` task in Gradle build file.

More complete sample can be found [here](sample-gradle).
Gradle plugin has the same features as Maven plugin, for detailed description see Maven generated [site](http://vojtechhabarta.github.io/typescript-generator/maven/typescript-generator-maven-plugin/generate-mojo.html). 


Direct invocation
-----------------
If you do not use Maven or Gradle you can invoke typescript-generator directly using `TypeScriptGenerator.generateTypeScript()` method.


Input classes
-------------
Input classes can be specified using several parameters:
- **`classes`** - list of fully qualified class names, includes all listed classes and their dependencies, `$` character is used for nested classes like `com.example.ClassName$NestedClassName`
- **`classPatterns`** - list of glob patterns like `com.example.*Json`, includes all classes matched by the pattern, supported are `*` and `**` wildcards
- **`classesFromJaxrsApplication`** - fully qualified name of JAX-RS application class, all classes used by application resources will be included, recommended if you have JAX-RS application class
- **`classesFromAutomaticJaxrsApplication`** - value `true` will include classes from automatically discovered REST resources, recommended if you have JAX-RS application without `Application` subclass
- **`excludeClasses`** - list of fully qualified class names, excluded classes will be mapped to TypeScript `any` type, if excluded class is a resource then this resource will not be scanned for used classes

> Note: it is possible to use multiple parameters at the same time.

For more details see [Class Names Glob Patterns](../../wiki/Class-Names-Glob-Patterns) and [JAX RS Application](../../wiki/JAX-RS-Application) Wiki pages.


Output parameters
-----------------
Output is configured using several parameters:
- `outputKind` (required parameter) - determines if and how module will be generated
    - values are: `global`, `module`, `ambientModule`
- `outputFileType` - specifies TypeScript file type
    - values are: `declarationFile` (.d.ts) or `implementationFile` (.ts)
- `outputFile` - specifies path and name of output file

For more details see [Modules and Namespaces](http://vojtechhabarta.github.io/typescript-generator/doc/ModulesAndNamespaces.html) page.


Download
--------
Releases are available from Maven Central Repository.
[Search](http://search.maven.org/#search%7Cga%7C1%7Ccz.habarta.typescript-generator) for dependency information for your build tool
or download [typescript-generator-core](https://repo1.maven.org/maven2/cz/habarta/typescript-generator/typescript-generator-core) directly.


Wiki
----
For more detailed description of some topics see [Wiki pages](../../wiki).


Architecture
------------

`TypeScriptGenerator` has 3 main parts (`ModelParser`, `ModelCompiler` and `Emitter`) which work together to produce TypeScript declarations for specified Java classes.

```
           (Model)            (TsModel)
ModelParser  ==>  ModelCompiler  ==>  Emitter
         |         |
         V         V
        TypeProcessor
```

- `ModelParser` reads Java JSON classes and their properties using Java reflections and creates `Model`.
  It uses `TypeProcessor`s for finding used classes.
  For example if property type is `List<Person>` it discovers that `Person` class should be also parsed.
  `ModelParser`s are specific for each JSON library (for example `Jackson2Parser`).
- `ModelCompiler` transforms Java model to TypeScript model (`Model` class to `TsModel` class).
  It uses `TypeProcessor`s for mapping Java types to TypeScript types (for example for `int` returns `number`).
- `Emitter` takes `TsModel` and produces TypeScript declaration file.


Links
-----

- http://www.rainerhahnekamp.com/type-safe-endpoints-with-typescript-and-java - blog post about using typescript-generator not only with Spring MVC
- http://www.jsweet.org/10-reasons-to-use-jsweet - blog post about JSweet transpiler mentions typescript-generator
- https://github.com/raphaeljolivet/java2typescript - tool similar to typescript-generator


Contributing
------------

- current major version supports Java 8 and later (version 1 supported Java 7 and 8)
- keep pull requests small and focused ([10 tips for better Pull Requests](http://blog.ploeh.dk/2015/01/15/10-tips-for-better-pull-requests/))
- do not add dependencies unless previously discussed in issue

### Code formatting

- use 4 spaces for indentation in Java files
- sort java imports alphabetically (including static imports), you can use wildcards
- please do not reformat whole files in IDE
