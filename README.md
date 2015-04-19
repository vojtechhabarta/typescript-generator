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
export interface Person {
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

Primarily typescript-generator is used as Maven plugin:
``` xml
<plugin>
    <groupId>cz.habarta.typescript-generator</groupId>
    <artifactId>typescript-generator-maven-plugin</artifactId>
    <version>1.1.71</version>
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
                <outputFile>target/rest.d.ts</outputFile>
            </configuration>
        </execution>
    </executions>
</plugin>
```

It can also be invoked directly using `TypeScriptGenerator.generateTypeScript()` method.
