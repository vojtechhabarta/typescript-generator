
package cz.habarta.typescript.generator.parser;


public class ClassWithUsage {
    
    public final Class<?> beanClass;
    public final String usedInProperty;
    public final Class<?> usedInClass;

    public ClassWithUsage(Class<?> beanClass, String usedInProperty, Class<?> usedInClass) {
        this.beanClass = beanClass;
        this.usedInProperty = usedInProperty;
        this.usedInClass = usedInClass;
    }

    public String usage() {
        return usedInClass.getSimpleName() + "." + usedInProperty;
    }

}
