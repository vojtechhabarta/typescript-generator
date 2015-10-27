
package cz.habarta.typescript.generator.gradle;

import java.util.*;
import org.gradle.api.*;


public class TypeScriptGeneratorPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        final Task task = project.task(Collections.singletonMap(Task.TASK_TYPE, GenerateTask.class), "generateTypeScript");
        task.dependsOn("compileJava");
        for (Task classesTask : project.getTasksByName("classes", false)) {
            classesTask.dependsOn(task);
        }
    }
    
}
