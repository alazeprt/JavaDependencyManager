package com.alazeprt;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A utility class for invoking instance and static methods using reflection.
 *
 * @author alazeprt
 */
public class DependencyClass {
    private final Object object;
    /**
     * Constructs an instance of DependencyClass with the specified object.
     *
     * @param object The object on which methods will be invoked.
     */
    public DependencyClass(Object object) {
        this.object = object;
    }

    /**
     * Invokes an instance method on the specified object.
     *
     * @param methodName The name of the method to invoke.
     * @param args       The arguments to pass to the method.
     * @return The result of invoking the method.
     * @throws InvocationTargetException If the method throws an exception.
     * @throws IllegalAccessException    If access to the method is denied.
     */
    public Object runMethod(String methodName, Object... args) throws InvocationTargetException, IllegalAccessException {
        Class<?> targetClass = object.getClass();
        Method method = DependencyLoader.getConfirmMethod(targetClass, methodName, args);
        if(method == null) {
            return null;
        }
        Object result = method.invoke(object, args);
        if (method.getReturnType().equals(Void.TYPE)) {
            return null;
        }
        return method.getReturnType().cast(result);
    }

    /**
     * Invokes a static method on the specified class using the provided class loader.
     *
     * @param loader     The DependencyLoader instance used for class loading.
     * @param className  The fully qualified class name containing the static method.
     * @param methodName The name of the static method to invoke.
     * @param args       The arguments to pass to the method.
     * @return The result of invoking the static method.
     * @throws ClassNotFoundException    If the class is not found.
     * @throws InvocationTargetException If the method throws an exception.
     * @throws IllegalAccessException    If access to the method is denied.
     */
    public static Object runStaticMethod(DependencyLoader loader, String className, String methodName, Object... args)
            throws ClassNotFoundException, InvocationTargetException, IllegalAccessException {
        Class<?> targetClass = loader.getClassLoader().loadClass(className);

        Method method = DependencyLoader.getConfirmMethod(targetClass, methodName, args);

        if(method == null) {
            return null;
        }

        Object result = method.invoke(null, args);

        if (method.getReturnType().equals(Void.TYPE)) {
            return null;
        }

        return method.getReturnType().cast(result);
    }

    /**
     * Invokes a static method on the class of the underlying object.
     *
     * @param methodName The name of the static method to invoke.
     * @param args       The arguments to pass to the method.
     * @return The result of invoking the static method.
     * @throws InvocationTargetException If the method throws an exception.
     * @throws IllegalAccessException    If access to the method is denied.
     */
    public Object runStaticMethod(String methodName, Object... args)
            throws InvocationTargetException, IllegalAccessException {
        Class<?> targetClass = object.getClass();

        Method method = DependencyLoader.getConfirmMethod(targetClass, methodName, args);

        if(method == null) {
            return null;
        }

        Object result = method.invoke(null, args);

        if (method.getReturnType().equals(Void.TYPE)) {
            return null;
        }

        return method.getReturnType().cast(result);
    }

    /**
     * Retrieves the object stored in the class.
     *
     * @return the object stored in the class
     */
    public Object getObject() {
        return object;
    }
}