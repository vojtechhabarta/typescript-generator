
package cz.habarta.typescript.generator.util;

import java.lang.annotation.Annotation;


@FunctionalInterface
public interface AnnotationGetter {
    public <A extends Annotation> A getAnnotation(Class<A> annotationClass);
}
