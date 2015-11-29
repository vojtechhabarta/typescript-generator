package cz.habarta.typescript.generator.util;

import java.lang.reflect.Field;
import java.lang.reflect.TypeVariable;
import java.util.List;

import com.google.api.client.util.Lists;

import sun.reflect.generics.repository.ClassRepository;

public class ClassUtils {

    private ClassUtils() {
        // nope
    }

    // Class A<T, V>
    // =>
    // ["A", "V"]
    public static List<String> getGenericDeclarationNames(Class<?> clazz) {
        try {
            Field genericInfoField = clazz.getClass().getDeclaredField("genericInfo");
            genericInfoField.setAccessible(true);
            ClassRepository cr = (ClassRepository) genericInfoField.get(clazz);
            List<String> ret = Lists.newArrayList();
            if (cr != null) {
                for(TypeVariable<?> typeVariable : cr.getTypeParameters()) {
                    ret.add(typeVariable.getName());
                }
            }
            return ret;
        } catch(RuntimeException | IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

}
