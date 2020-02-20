
package cz.habarta.typescript.generator.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.Objects;


public class PropertyMember {

    private final AnnotatedElement annotatedElement;
    private final Type type;
    private final AnnotatedType annotatedType;
    private final AnnotationGetter annotationGetter;

    public PropertyMember(AnnotatedElement annotatedElement, Type type, AnnotatedType annotatedType, AnnotationGetter annotationGetter) {
        this.annotatedElement = Objects.requireNonNull(annotatedElement);
        this.type = Objects.requireNonNull(type);
        this.annotatedType = Objects.requireNonNull(annotatedType);
        this.annotationGetter = annotationGetter;
    }

    public Type getType() {
        return type;
    }

    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        final A annotation = annotationGetter != null
                ? annotationGetter.getAnnotation(annotationClass)
                : annotatedElement.getAnnotation(annotationClass);
        return annotation != null
                ? annotation
                : annotatedType.getAnnotation(annotationClass);
    }

}
