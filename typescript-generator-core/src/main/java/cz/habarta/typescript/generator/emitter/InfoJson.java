
package cz.habarta.typescript.generator.emitter;

import java.util.List;


public class InfoJson {

    public List<ClassInfo> classes;


    public static class ClassInfo {
        public String javaClass;
        public String typeName;
    }

}
