
package cz.habarta.typescript.generator.util;

import java.lang.annotation.Annotation;
import org.jspecify.annotations.Nullable;


@FunctionalInterface
public interface AnnotationGetter {
    public <A extends Annotation> @Nullable A getAnnotation(Class<A> annotationClass);
}
