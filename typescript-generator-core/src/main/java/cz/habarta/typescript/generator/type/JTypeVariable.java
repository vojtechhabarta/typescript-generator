
package cz.habarta.typescript.generator.type;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Objects;


public class JTypeVariable<D extends GenericDeclaration> implements TypeVariable<D> {

    private final D genericDeclaration;  // should not be null but for Kotlin KTypeParameter we don't have it
    private final String name;
    private Type[] bounds;
    private final AnnotatedType[] annotatedBounds;
    private final Annotation[] annotations;
    private final Annotation[] declaredAnnotations;

    public JTypeVariable(D genericDeclaration, String name) {
        this(genericDeclaration, name, null, null, null, null);
    }

    public JTypeVariable(D genericDeclaration, String name, Type[] bounds, AnnotatedType[] annotatedBounds, Annotation[] annotations, Annotation[] declaredAnnotations) {
        this.genericDeclaration = genericDeclaration;
        this.name = Objects.requireNonNull(name, "name");
        this.bounds = bounds != null ? bounds : new Type[0];
        this.annotatedBounds = annotatedBounds != null ? annotatedBounds : new AnnotatedType[0];
        this.annotations = annotations != null ? annotations : new Annotation[0];
        this.declaredAnnotations = declaredAnnotations != null ? declaredAnnotations : this.annotations;
    }

    @Override
    public Type[] getBounds() {
        return bounds;
    }

    public void setBounds(Type[] bounds) {
        this.bounds = bounds;
    }

    @Override
    public D getGenericDeclaration() {
        return genericDeclaration;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public AnnotatedType[] getAnnotatedBounds() {
        return annotatedBounds;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        for (Annotation annotation : getAnnotations()) {
            if (annotationClass.isInstance(annotation)) {
                return (T) annotation;
            }
        }
        return null;
    }

    @Override
    public Annotation[] getAnnotations() {
        return annotations;
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return getAnnotations();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(genericDeclaration) ^ Objects.hashCode(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof TypeVariable) {
            final TypeVariable<?> that = (TypeVariable<?>) obj;
            return Objects.equals(genericDeclaration, that.getGenericDeclaration()) &&
                    Objects.equals(name, that.getName());
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return getName();
    }

}
