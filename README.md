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


Maven
-----

In Maven build you can use `typescript-generator-maven-plugin` like this:
``` xml
<plugin>
    <groupId>cz.habarta.typescript-generator</groupId>
    <artifactId>typescript-generator-maven-plugin</artifactId>
    <version>1.2.x</version>
    <executions>
        <execution>
            <id>generate</id>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <classes>
                    <class>cz.habarta.typescript.generator.Person</class>
                </classes>
                <namespace>Rest</namespace>
                <outputFile>target/rest.d.ts</outputFile>
            </configuration>
        </execution>
    </executions>
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
        classpath group: 'cz.habarta.typescript-generator', name: 'typescript-generator-gradle-plugin', version: '1.2.x'
    }
}
generateTypeScript {
    outputFile = 'build/sample.d.ts'
    classes = [
        'cz.habarta.typescript.generator.sample.Person'
    ]
    jsonLibrary = 'jackson2'
    namespace = 'Rest';
//    module = 'my-module'
//    declarePropertiesAsOptional = false
//    removeTypeNameSuffix = 'Json'
//    mapDate = 'asNumber'
}
```

More complete sample can be found [here](sample-gradle).
Gradle plugin has the same features as Maven plugin, for detailed description see Maven generated [site](http://vojtechhabarta.github.io/typescript-generator/maven/typescript-generator-maven-plugin/generate-mojo.html). 


Direct invocation
-----------------
If you do not use Maven or Gradle you can invoke typescript-generator directly using `TypeScriptGenerator.generateTypeScript()` method.

Releases are available from [Maven Central Repository](http://search.maven.org/#search%7Cga%7C1%7Ccz.habarta.typescript-generator).


Contributing
------------

- this project targets Java 7
- keep pull requests small and focused ([10 tips for better Pull Requests](http://blog.ploeh.dk/2015/01/15/10-tips-for-better-pull-requests/))
- do not add dependencies unless previously discussed in issue

### Code formatting

- use 4 spaces for indentation in Java files
- sort java imports alphabetically, you can use wildcards
- please do not reformat whole files in IDE
