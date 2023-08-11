package com.alazeprt;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Utility class for loading classes, constructing objects, and invoking methods with external dependencies.
 */
public class DependencyLoader {
    private URLClassLoader classLoader;

    /**
     * Creates a class loader based on the specified file.
     *
     * @param libPath The path to the directory containing external dependencies.
     * @throws MalformedURLException If the provided URL is malformed.
     */
    public DependencyLoader(String libPath) throws MalformedURLException {
        this.classLoader = new URLClassLoader(new URL[]{new File(libPath).toURI().toURL()});
    }

    /**
     * Constructs an instance of the specified class name.
     *
     * @param classname The fully qualified class name to instantiate.
     * @return An instance of the specified class.
     * @throws ClassNotFoundException    If the class is not found.
     * @throws InstantiationException    If an instance of the class cannot be created.
     * @throws IllegalAccessException    If access to the class or its constructor is denied.
     */
    public Object construct(String classname) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        return classLoader.loadClass(classname).newInstance();
    }

    /**
     * Adds additional paths to the class loader.
     *
     * @param libPath The path to the directory containing more external dependencies.
     * @throws MalformedURLException If the provided URL is malformed.
     */
    public void loadMoreLibrary(String libPath) throws MalformedURLException {
        URL[] currentUrls = classLoader.getURLs();
        URL[] newUrls = new URL[currentUrls.length + 1];
        System.arraycopy(currentUrls, 0, newUrls, 0, currentUrls.length);
        newUrls[currentUrls.length] = new File(libPath).toURI().toURL();
        this.classLoader = new URLClassLoader(newUrls);
    }

    /**
     * Constructs an instance of the specified class with the given arguments.
     *
     * @param className The fully qualified class name to instantiate.
     * @param args      The arguments to pass to the constructor.
     * @return An instance of the specified class.
     * @throws ClassNotFoundException    If the class is not found.
     * @throws NoSuchMethodException     If the constructor with the specified parameter types is not found.
     * @throws InstantiationException    If an instance of the class cannot be created.
     * @throws IllegalAccessException    If access to the class or its constructor is denied.
     * @throws InvocationTargetException If the constructor throws an exception.
     */
    public Object construct(String className, Object... args) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Class<?> targetClass = classLoader.loadClass(className);
        Class<?>[] parameterTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            parameterTypes[i] = args[i].getClass();
        }
        Constructor<?> constructor = targetClass.getConstructor(parameterTypes);
        return constructor.newInstance(args);
    }

    /**
     * Loads the specified class based on external dependencies.
     *
     * @param className The fully qualified class name to load.
     * @return The loaded class.
     * @throws ClassNotFoundException If the class is not found.
     */
    public Class<?> getLocalClass(String className) throws ClassNotFoundException {
        return classLoader.loadClass(className);
    }

    /**
     * Invokes an instance method on the specified object.
     *
     * @param targetObject The object on which to invoke the method.
     * @param methodName   The name of the method to invoke.
     * @param returnType   The expected return type of the method.
     * @param args         The arguments to pass to the method.
     * @return The result of invoking the method.
     * @throws NoSuchMethodException     If the method with the specified name and parameter types is not found.
     * @throws InvocationTargetException If the method throws an exception.
     * @throws IllegalAccessException    If access to the method is denied.
     */
    public Object runMethod(Object targetObject, String methodName, Class<?> returnType, Object... args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class<?> targetClass = targetObject.getClass();
        Class<?>[] parameterTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            parameterTypes[i] = args[i].getClass();
        }
        Method method = targetClass.getMethod(methodName, parameterTypes);
        Object result = method.invoke(targetObject, args);
        if (returnType.equals(Void.TYPE)) {
            return null;
        }
        return returnType.cast(result);
    }

    /**
     * Invokes a static method on the specified class.
     *
     * @param className  The fully qualified class name containing the static method.
     * @param methodName The name of the static method to invoke.
     * @param returnType The expected return type of the method.
     * @param args       The arguments to pass to the method.
     * @return The result of invoking the static method.
     * @throws ClassNotFoundException    If the class is not found.
     * @throws NoSuchMethodException     If the method with the specified name and parameter types is not found.
     * @throws InvocationTargetException If the method throws an exception.
     * @throws IllegalAccessException    If access to the method is denied.
     */
    public Object runStaticMethod(String className, String methodName, Class<?> returnType, Object... args)
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class<?> targetClass = classLoader.loadClass(className);

        Class<?>[] parameterTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            parameterTypes[i] = args[i].getClass();
        }

        Method method = targetClass.getMethod(methodName, parameterTypes);

        Object result = method.invoke(null, args);

        if (returnType.equals(Void.TYPE)) {
            return null;
        }

        return returnType.cast(result);
    }
}