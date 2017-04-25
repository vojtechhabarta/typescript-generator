package cz.habarta.typescript.generator.parser;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.TypeProcessor;

/* https://wiki.openjdk.java.net/display/Nashorn/Nashorn+Documentation
 * http://docs.oracle.com/javase/8/docs/technotes/guides/scripting/prog_guide/index.html
 * https://wiki.openjdk.java.net/display/Nashorn/Nashorn+extensions */
public class NashornJavaParser extends ModelParser {
    
    private final ObjectMapper objectMapper = new ObjectMapper();    

    public NashornJavaParser(Settings settings, TypeProcessor typeProcessor) {
        super(settings, typeProcessor);
        objectMapper.registerModules(ObjectMapper.findModules(settings.classLoader));
    }
    
    @Override
    protected BeanModel parseBean(SourceType<Class<?>> sourceClass) {
        final List<PropertyModel> properties = new ArrayList<>();
        final List<MethodModel> methods = new ArrayList<>();
        
        /* Public fields */
        for (Field field : sourceClass.type.getDeclaredFields()) {
            
            /* only include public fields */
            if (!Modifier.isPublic(field.getModifiers())) 
                continue;
                
            Type type = field.getGenericType();
            List<Class<?>> classes = discoverClassesUsedInType(type);
            for (Class<?> cls : classes) {
                addBeanToQueue(new SourceType<>(cls, sourceClass.type, field.getName()));
            }
            if (type instanceof ParameterizedType) {
                ParameterizedType aType = (ParameterizedType) type;
                Type[] parameterArgTypes = aType.getActualTypeArguments();
                for(Type parameterArgType : parameterArgTypes){
                    addBeanToQueue(new SourceType<>(parameterArgType, sourceClass.type, field.getName()));
                }
            }
            
            properties.add(new PropertyModel(field.getName(), type, false, field, null, null));
        }

        /* Private fields that have a getter or setter method become properties */
        try {
            BeanInfo info = Introspector.getBeanInfo(sourceClass.type);
            for (PropertyDescriptor pd : info.getPropertyDescriptors()) {            
                if (pd.getReadMethod() != null) {
                    String propertyName = pd.getName();
                    if (propertyName.equals("class")) {
                        continue;
                    }                    
                    Type type = pd.getReadMethod().getGenericReturnType();
                    List<Class<?>> classes = discoverClassesUsedInType(type);
                    for (Class<?> cls : classes) {
                        addBeanToQueue(new SourceType<>(cls, sourceClass.type, propertyName));
                    }
                    if (type instanceof ParameterizedType) {
                        ParameterizedType aType = (ParameterizedType) type;
                        Type[] parameterArgTypes = aType.getActualTypeArguments();
                        for(Type parameterArgType : parameterArgTypes){
                            addBeanToQueue(new SourceType<>(parameterArgType, sourceClass.type, propertyName));
                        }
                    }
                    properties.add(new PropertyModel(propertyName, type, false, pd.getReadMethod(), null, null));
                }
            }
        }
        catch (IntrospectionException e1) {
            /* ignore */
        }
        
        /* Public methods */
        for (Method method : sourceClass.type.getDeclaredMethods()) {
            
            /* only include public methods */            
            if (!Modifier.isPublic(method.getModifiers())) 
                continue;
            
            /* return type analyzed */
            Type returnType = method.getGenericReturnType();
            List<Class<?>> classes = discoverClassesUsedInType(returnType);
            for (Class<?> cls : classes) {
                addBeanToQueue(new SourceType<>(cls, sourceClass.type, method.getName()));
                
                if (returnType instanceof ParameterizedType) {
                    ParameterizedType aType = (ParameterizedType) returnType;
                    Type[] parameterArgTypes = aType.getActualTypeArguments();
                    for(Type parameterArgType : parameterArgTypes){
                        addBeanToQueue(new SourceType<>(parameterArgType, sourceClass.type, method.getName()));
                    }
                }
            }            
            
            /* parameters and generic parameters */
            List<MethodParameterModel> parameters = new ArrayList<>();      
            for (Parameter parameter : method.getParameters()) {
                Type parameterizedType = parameter.getParameterizedType();
                parameters.add(new MethodParameterModel(parameter.getName(), parameterizedType));
                addBeanToQueue(new SourceType<>(parameter.getType(), sourceClass.type, method.getName()));
                
                if (parameterizedType instanceof ParameterizedType) {
                    ParameterizedType aType = (ParameterizedType) parameterizedType;
                    Type[] parameterArgTypes = aType.getActualTypeArguments();
                    for(Type parameterArgType : parameterArgTypes){
                        addBeanToQueue(new SourceType<>(parameterArgType, sourceClass.type, method.getName()));
                    }
                }
            }
            methods.add(new MethodModel(sourceClass.type, method.getName(), parameters, returnType, null));            
        }
       
        final Type superclass = sourceClass.type.getGenericSuperclass() == Object.class ? null : sourceClass.type.getGenericSuperclass();
        if (superclass != null) {
            addBeanToQueue(new SourceType<>(superclass, sourceClass.type, "<superClass>"));
        }
        final List<Type> interfaces = Arrays.asList(sourceClass.type.getGenericInterfaces());
        for (Type aInterface : interfaces) {
            addBeanToQueue(new SourceType<>(aInterface, sourceClass.type, "<interface>"));
        }
        return new BeanModel(sourceClass.type, superclass, null, null, null, interfaces, properties, methods, null);
    }
}