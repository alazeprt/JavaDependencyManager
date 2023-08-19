# JavaDependencyManager
##### [English](./README.md) | 简体中文

### 关于

JavaDependencyManager是由alazeprt创作的一个提供了许多有关依赖的功能的库，这个库将繁琐的有关依赖的功能包装在多个类中，使用户可以轻松地调用这些库来使用依赖，不需要那么多繁琐的操作。

### 导入

JavaDependencyManager已经发布到了Maven中心，你可以在[这里](https://mvnrepository.com/artifact/com.alazeprt/JavaDependencyManager)查看关于它的导入方式，下面列举了一些常见的导入方式。

##### Maven:
```xml
<dependency>
    <groupId>com.alazeprt</groupId>
    <artifactId>JavaDependencyManager</artifactId>
    <version>1.2.2</version>
</dependency>
```

##### Gradle (Groovy) :
```groovy
dependencies {
    implementation 'com.alazeprt:JavaDependencyManager:1.2.2'
}
```

##### Gradle (Kotlin) :
```kotlin
dependencies {
    implementation("com.alazeprt:JavaDependencyManager:1.2.2")
}
```

### 使用方法

#### 下载依赖

如果要下载依赖，我们首先要声明一个`Dependency`变量，声明所需的参数的格式为`groupId:artifactId:version`，也可以将3个值提取出来分别作为参数传递，声明好之后使用`getSubDependencies()`方法来获取这个依赖下所有的子依赖，将其保存到一个列表中，接着使用`DependencyDownloader`类的`downloadAll()`方法来下载所有依赖，这个方法的三个参数分别代表要下载的所有依赖、下载后导出的路径以及每个依赖下载的线程数。

例如，我们需要下载gson依赖的2.10.1版本，可以使用以下代码：

```java
import java.util.List;

public class Test {
    public static void main(String[] args) throws Exception {
        Dependency dependency = new Dependency("com.google.code.gson:gson:2.10.1"); // 最快捷的定义方式
        Dependency dependency1 = new Dependency("com.google.code.gson", "gson", "2.10.1"); // 这样也可以
        List<Dependency> list = dependency.getSubDependencies(); // 遍历所有此依赖项的子依赖
        DependencyDownloader.downloadAll(list, "./libs", 8); // 下载所有依赖到./libs文件夹, 每个依赖使用8线程下载
    }
}
```

#### 使用依赖

##### 构造外部变量 & 使用外部变量的方法

在下载完依赖之后，我们就可以引入这个依赖并使用了。

首先，我们定义一个`DependencyLoader`变量，通过这个变量可以调用类，在定义变量的时候需要传入两个参数，分别是依赖所在的文件夹路径以及所有所需的依赖。

例如，我们想使用Gson库运行下面这段代码：

```java
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class Test {
    public static void main(String[] args) {
        Gson gson = new Gson();
        Map<String, String> map = new HashMap<>();
        map.put("gson", "2.10.1");
        map.put("JavaDependencyManager", "1.2");
        map.put("log4j-core", "2.20.0");
        JsonObject jsonObject = gson.fromJson(gson.toJson(map), JsonObject.class);
        System.out.println(jsonObject.get("gson").toString());
        System.out.println(gson.toJson(jsonObject));
    }
}
```

我们可以将代码转换为：

```java
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Test {
    public static void main(String[] args) throws Exception {
        Dependency dependency = new Dependency("com.google.code.gson:gson:2.10.1");
        List<Dependency> list = dependency.getSubDependencies();
        DependencyDownloader.downloadAll(list, "./libs", 8);
        DependencyLoader loader = new DependencyLoader("./libs/", list); // 加载libs文件夹中的所有依赖
        DependencyClass gson = loader.construct("com.google.gson.Gson"); // 声明一个Gson类型变量
        Map<String, String> map = new HashMap<>(); // 声明一个HashMap, 存Json数据
        map.put("gson", "2.10.1");
        map.put("JavaDependencyManager", "1.2");
        map.put("log4j-core", "2.20.0");
        // 声明一个JsonObject类型变量，赋值为解析后的HashMap数据
        DependencyClass jsonObject = new DependencyClass(gson.runMethod("fromJson", gson.runMethod("toJson", map), loader.getLocalClass("com.google.gson.JsonObject")));
        System.out.println(jsonObject.runMethod("get", "gson").toString()); // 调用方法
        System.out.println(gson.runMethod("toJson", jsonObject.getObject()).toString()); // 同上
    }
}
```

其中我们可以使用这个`DependencyLoader`类的`construct()`方法来调用这个类的构造方法，代码中通过`loader.construct("com.google.gson.Gson")`来调用Gson类的构造方法，这个`construct()`方法会返回一个DependencyClass类型的变量，表示构造的类。

接着如果我们要调用某个类的方法，可以使用`DependencyClass`类的`runMethod()`方法来调用，其中第一个参数为需要调用的方法名，后面的参数为调用此方法所需的参数。（注意：`runMethod()`方法不能用于调用静态方法，如果需要调用静态方法见下文，使用`runMethod()`来调用静态方法的后果本作者不承担）

##### 使用静态方法

例如，我们想使用log4j库来记录日志，原来的代码是这样的：

```java
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Test_Log4j2 {
    public static void main(String[] args) {
        final Logger logger = LogManager.getLogger();
        logger.info("Hello World!");
    }
}
```

我们可以将代码转换为：

```java
import java.util.List;

public class Test_Log4j2 {
    public static void main(String[] args) throws Exception {
        Dependency dependency = new Dependency("org.apache.logging.log4j:log4j-core:2.20.0");
        List<Dependency> list = dependency.getSubDependencies();
        DependencyDownloader.downloadAll(list, "./libs", 8);
        DependencyLoader loader = new DependencyLoader("./libs/", list); // 加载libs文件夹中的所有依赖
        DependencyClass logger = new DependencyClass(loader.runStaticMethod("org.apache.logging.log4j.LogManager", "getLogger", Test.class));
        logger.runMethod("info", "Hello World!");
    }
}
```

其中，我们可以使用`DependencyLoader`类中的`runStaticMethod()`方法来调用一个静态方法，其中第一个参数为此静态方法所在的类，第二个参数为方法名，后面的参数均为调用此方法所需的参数。

#### JavaDoc

你可以在[这里](https://docs.alazeprt.com/)查看此项目的JavaDoc。

### 问题追踪

如果你有什么问题、想法或发现了什么bug，你可以到[这里](https://github.com/alazeprt/JavaDependencyManager/issues)提交。

### 协议

本项目使用了GPL-3.0协议。