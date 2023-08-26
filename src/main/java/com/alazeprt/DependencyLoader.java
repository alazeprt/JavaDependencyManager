package com.alazeprt;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for loading classes, constructing objects, and invoking methods with external dependencies.
 *
 * @author alazeprt
 */
public class DependencyLoader {
    private URLClassLoader classLoader;

    /**
     * Creates a class loader based on the specified file.
     *
     * @param libPath The path to the directory containing external dependencies.
     * @param list    List of Dependency instances representing external dependencies.
     * @throws MalformedURLException If the provided URL is malformed.
     */
    public DependencyLoader(String libPath, List<Dependency> list) throws MalformedURLException {
        if(!libPath.endsWith("/") || !libPath.endsWith("\\")) {
            libPath += "\\";
        }
        URL[] urls = new URL[list.size()];
        for (int i = 0; i < list.size(); i++) {
            if(list.get(i).isExternal()) {
                urls[i] = new File(libPath + list.get(i).getDependency().split(":")[1] + "-" + list.get(i).getDependency().split(":")[2] + ".jar").toURI().toURL();
            } else {
                urls[i] = new URL(list.get(i).getDependency());
            }
        }
        this.classLoader = new URLClassLoader(urls);
    }

    /**
     * Creates a class loader based on the specified directory.
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
     * @throws NoSuchMethodException     If the constructor with no parameters is not found.
     * @throws InvocationTargetException If the constructor throws an exception.
     */
    public DependencyClass construct(String classname) throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        return new DependencyClass(classLoader.loadClass(classname).getDeclaredConstructor().newInstance());
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
    public DependencyClass construct(String className, Object... args) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Class<?> targetClass = classLoader.loadClass(className);
        Constructor<?> constructor = getCompatibleConstructor(targetClass, args);
        return new DependencyClass(constructor.newInstance(args));
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
     * Invokes a static method on the specified class.
     *
     * @param className  The fully qualified class name containing the static method.
     * @param methodName The name of the static method to invoke.
     * @param args       The arguments to pass to the method.
     * @return The result of invoking the static method.
     * @throws ClassNotFoundException    If the class is not found.
     * @throws NoSuchMethodException     If the method with the specified name and parameter types is not found.
     * @throws InvocationTargetException If the method throws an exception.
     * @throws IllegalAccessException    If access to the method is denied.
     */
    public Object runStaticMethod(String className, String methodName, Object... args)
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class<?> targetClass = classLoader.loadClass(className);

        Method method = getConfirmMethod(targetClass, methodName, args);

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
     * Returns the underlying URLClassLoader instance.
     *
     * @return The underlying URLClassLoader.
     */
    public URLClassLoader getClassLoader() {
        return classLoader;
    }

    private static final Map<Class<?>, Class<?>> primitiveToWrapperMap = new HashMap<>();

    static {
        primitiveToWrapperMap.put(boolean.class, Boolean.class);
        primitiveToWrapperMap.put(byte.class, Byte.class);
        primitiveToWrapperMap.put(short.class, Short.class);
        primitiveToWrapperMap.put(char.class, Character.class);
        primitiveToWrapperMap.put(int.class, Integer.class);
        primitiveToWrapperMap.put(long.class, Long.class);
        primitiveToWrapperMap.put(float.class, Float.class);
        primitiveToWrapperMap.put(double.class, Double.class);
    }

    public static Method getConfirmMethod(Class<?> targetClass, String methodName, Object... args) {
        Method[] methods = targetClass.getMethods();
        for (Method method : methods) {
            if (!method.getName().equals(methodName) || args.length != method.getParameterCount()) {
                continue;
            }
            Class<?>[] classes = method.getParameterTypes();
            boolean isMethod = true;
            for (int i = 0; i < args.length; i++) {
                if(args[i].getClass().equals(DependencyClass.class)) {
                    args[i] = ((DependencyClass) args[i]).getObject();
                }
                if (!isParameterCompatible(classes[i], args[i])) {
                    isMethod = false;
                    break;
                }
            }
            if (isMethod) {
                return method;
            }
        }
        return null;
    }

    private static boolean isParameterCompatible(Class<?> parameterType, Object arg) {
        if (parameterType.isPrimitive()) {
            Class<?> wrapperType = primitiveToWrapperMap.get(parameterType);
            if(wrapperType == null) {
                wrapperType = parameterType;
            }
            return wrapperType.isInstance(arg);
        } else {
            return parameterType.isInstance(arg);
        }
    }

    static Constructor<?> getCompatibleConstructor(Class<?> targetClass, Object... args) {
        Constructor<?>[] constructors = targetClass.getConstructors();
        for (Constructor<?> constructor : constructors) {
            if (constructor.getParameterCount() != args.length) {
                continue;
            }
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            boolean isCompatible = true;
            for (int i = 0; i < args.length; i++) {
                if (!isParameterCompatible(parameterTypes[i], args[i])) {
                    isCompatible = false;
                    break;
                }
            }
            if (isCompatible) {
                return constructor;
            }
        }
        return null;
    }
}