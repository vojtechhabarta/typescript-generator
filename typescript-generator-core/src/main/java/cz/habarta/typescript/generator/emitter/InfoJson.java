
package cz.habarta.typescript.generator.emitter;

import java.util.List;


public class InfoJson {

    public List<ClassInfo> classes;

    public InfoJson(List<ClassInfo> classes) {
        this.classes = classes;
    }

    public static class ClassInfo {
        public String javaClass;
        public String typeName;

        public ClassInfo(String javaClass, String typeName) {
            this.javaClass = javaClass;
            this.typeName = typeName;
        }
    }

}
