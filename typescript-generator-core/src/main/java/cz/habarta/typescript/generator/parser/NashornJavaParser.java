package cz.habarta.typescript.generator.parser;

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
        
        for (Field field : sourceClass.type.getDeclaredFields()) {
            
            /* only include public fields */
            if (!Modifier.isPublic(field.getModifiers())) 
                continue;
                
            Type type = field.getGenericType();
            List<Class<?>> classes = discoverClassesUsedInType(type);
            for (Class<?> cls : classes) {
                addBeanToQueue(new SourceType<>(cls, sourceClass.type, field.getName()));
            }
            properties.add(new PropertyModel(field.getName(), type, false, field, null, null));
        }
        
        for (Method method : sourceClass.type.getDeclaredMethods()) {
            
            /* only include public methods */            
            if (!Modifier.isPublic(method.getModifiers())) 
                continue;
            
            /* method is a property getter */
            PropertyModel propertyGetter = parseGetterMethod(sourceClass.type, method);
            if (propertyGetter != null) {
                List<Class<?>> classes = discoverClassesUsedInType(propertyGetter.getType());
                for (Class<?> cls : classes) {
                    addBeanToQueue(new SourceType<>(cls, sourceClass.type, propertyGetter.getName()));
                }
                properties.add(propertyGetter);   
                continue;
            }            
            
            /* method is a property setter */
            PropertyModel propertySetter = parseSetterMethod(sourceClass.type, method);
            if (propertySetter != null) {
                List<Class<?>> classes = discoverClassesUsedInType(propertySetter.getType());
                for (Class<?> cls : classes) {
                    addBeanToQueue(new SourceType<>(cls, sourceClass.type, propertySetter.getName()));
                }
                properties.add(propertySetter);    
                continue;
            }  
            
            /* plain method */
            Type returnType = method.getGenericReturnType();
            List<Class<?>> classes = discoverClassesUsedInType(returnType);
            for (Class<?> cls : classes) {
                addBeanToQueue(new SourceType<>(cls, sourceClass.type, method.getName()));
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
    
    private PropertyModel parseSetterMethod(Class<?> clazz, Method method) {
        String methodName = method.getName();
        
        // setters start with 'set'
        if (!methodName.startsWith("set") || methodName.length() < 4)
            return null;
        
        // setters return 'void'
        if (!method.getReturnType().equals(Void.TYPE))
            return null;
        
        // setters have a single argument
        if (method.getParameterCount() != 1)
            return null;
        
        // we have a winner
        String propertyName = methodName.substring(2);
        propertyName = propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1);
        Type propertyType = method.getParameterTypes()[0];
        
        return new PropertyModel(propertyName, propertyType, false, method, null, null);
    }
    
    private PropertyModel parseGetterMethod(Class<?> clazz, Method method) {
        String methodName = method.getName();
        
        // getters start with 'get'
        if (methodName.equalsIgnoreCase("getClass"))
            return null;
        
        if (!methodName.startsWith("get") && !methodName.startsWith("is"))
            return null;
        if (methodName.startsWith("get") && methodName.length() < 4)
            return null;        
        if (methodName.startsWith("is") && methodName.length() < 3)
            return null;
        
        // getters dont return 'void'
        if (method.getReturnType().equals(Void.TYPE))
            return null;
        
        // getters have no arguments
        if (method.getParameterCount() != 0)
            return null;
        
        // we might have a winner
        if (methodName.startsWith("is")) {
            
            // 'is' type getters always return a boolean
            if (!method.getReturnType().equals(boolean.class))
                return null;
            
            Type propertyType = method.getReturnType();
            String propertyName = methodName.substring(2);
            propertyName = propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1);
            
            return new PropertyModel(propertyName, propertyType, false, method, null, null);
        }
        else if (methodName.startsWith("get")) {
            
            Type propertyType = method.getReturnType();
            String propertyName = methodName.substring(3);
            propertyName = propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1);            
            
            return new PropertyModel(propertyName, propertyType, false, method, null, null);            
        }
        return null;
    }    
}