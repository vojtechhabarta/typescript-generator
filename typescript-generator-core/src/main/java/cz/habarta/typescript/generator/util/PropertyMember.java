
package cz.habarta.typescript.generator.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;


public abstract class PropertyMember {

    public abstract Type getType();

    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        final A annotation = getAnnotatedElement().getAnnotation(annotationClass);
        return annotation != null
                ? annotation
                : getAnnotatedType().getAnnotation(annotationClass);
    }

    protected abstract AnnotatedElement getAnnotatedElement();

    protected abstract AnnotatedType getAnnotatedType();


    public static class FieldPropertyMember extends PropertyMember {

        private final Field field;

        public FieldPropertyMember(Field field) {
            this.field = field;
        }

        @Override
        public Type getType() {
            return field.getGenericType();
        }

        @Override
        public AnnotatedElement getAnnotatedElement() {
            return field;
        }

        @Override
        public AnnotatedType getAnnotatedType() {
            return field.getAnnotatedType();
        }

    }

    public static class MethodPropertyMember extends PropertyMember {

        private final Method method;

        public MethodPropertyMember(Method method) {
            this.method = method;
        }

        @Override
        public Type getType() {
            return method.getGenericReturnType();
        }

        @Override
        public AnnotatedElement getAnnotatedElement() {
            return method;
        }

        @Override
        public AnnotatedType getAnnotatedType() {
            return method.getAnnotatedReturnType();
        }

    }

}
