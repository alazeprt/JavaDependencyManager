package com.alazeprt;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class DependencyLoader {
    private URLClassLoader classLoader;
    public DependencyLoader(String libPath) throws MalformedURLException {
        this.classLoader = new URLClassLoader(new URL[] {new File(libPath).toURI().toURL()});
    }

    public Object construct(String function) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        return classLoader.loadClass(function).newInstance();
    }

    public void loadMoreLibrary(String libPath) throws MalformedURLException {
        URL[] currentUrls = classLoader.getURLs();
        URL[] newUrls = new URL[currentUrls.length + 1];
        System.arraycopy(currentUrls, 0, newUrls, 0, currentUrls.length);
        newUrls[currentUrls.length] = new File(libPath).toURI().toURL();
        this.classLoader = new URLClassLoader(newUrls);
    }


    public Object construct(String className, Object... args) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Class<?> targetClass = classLoader.loadClass(className);

        // 获取参数类型数组
        Class<?>[] parameterTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            parameterTypes[i] = args[i].getClass();
        }

        // 获取对应的构造函数
        Constructor<?> constructor = targetClass.getConstructor(parameterTypes);

        // 调用构造函数创建对象
        return constructor.newInstance(args);
    }

    public Class<?> getLocalClass(String className) throws ClassNotFoundException {
        return classLoader.loadClass(className);
    }

    public Object runMethod(Object targetObject, String methodName, Class<?> returnType, Object... args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class<?> targetClass = targetObject.getClass();

        // 获取参数类型数组
        Class<?>[] parameterTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            parameterTypes[i] = args[i].getClass();
        }

        // 获取对应的方法
        Method method = targetClass.getMethod(methodName, parameterTypes);

        // 调用方法并返回结果
        Object result = method.invoke(targetObject, args);

        // 对于 void 返回类型，返回 null
        if (returnType.equals(Void.TYPE)) {
            return null;
        }

        // 强制转换返回值类型
        return returnType.cast(result);
    }

    public Object runStaticMethod(String className, String methodName, Class<?> returnType, Object... args)
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class<?> targetClass = classLoader.loadClass(className);

        // 获取参数类型数组
        Class<?>[] parameterTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            parameterTypes[i] = args[i].getClass();
        }

        // 获取对应的方法
        Method method = targetClass.getMethod(methodName, parameterTypes);

        // 调用方法并返回结果
        Object result = method.invoke(null, args);

        // 对于 void 返回类型，返回 null
        if (returnType.equals(Void.TYPE)) {
            return null;
        }

        // 强制转换返回值类型
        return returnType.cast(result);
    }
}