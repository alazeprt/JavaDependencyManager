# JavaDependencyManager
##### English | [简体中文](./README_zh.md)

### About

JavaDependencyManager is a library created by alazeprt that provides various functionalities related to dependencies. This library encapsulates complex dependency-related operations within multiple classes, making it easy for users to call these classes to utilize dependencies without the need for intricate procedures.

### Import

JavaDependencyManager has been published to Maven Central. You can check the import methods for it [here](https://mvnrepository.com/artifact/com.alazeprt/JavaDependencyManager). Below are some common import methods.

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

### Usage

#### Download Dependencies

To download dependencies, we first need to declare a `Dependency` variable. The format for declaring required parameters is `groupId:artifactId:version`. Alternatively, you can extract these three values and pass them as separate parameters. After declaration, use the `getSubDependencies()` method to retrieve all sub-dependencies of this dependency and store them in a list. Then, use the `downloadAll()` method of the `DependencyDownloader` class to download all dependencies. The three parameters of this method represent the dependencies to be downloaded, the export path after downloading, and the number of threads for downloading each dependency.

For example, to download the version 2.10.1 of the Gson dependency, you can use the following code:

```java
import java.util.List;

public class Test {
    public static void main(String[] args) throws Exception {
        Dependency dependency = new Dependency("com.google.code.gson:gson:2.10.1"); // The quickest way to define
        Dependency dependency1 = new Dependency("com.google.code.gson", "gson", "2.10.1"); // This approach works as well
        List<Dependency> list = dependency.getSubDependencies(); // Traverse all sub-dependencies of this dependency
        DependencyDownloader.downloadAll(list, "./libs", 8); // Download all dependencies to the ./libs folder, using 8 threads per dependency
    }
}
```

#### Using Dependencies

##### Constructing External Variables & Methods using External Variables

After downloading the dependencies, we can import and use them.

First, we define a `DependencyLoader` variable, which can be used to invoke classes. When defining the variable, you need to pass two parameters: the folder path where the dependencies are located and all required dependencies.

For example, if we want to use the Gson library to run the following code:

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

We can transform the code to:

```java
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Test {
    public static void main(String[] args) throws Exception {
        Dependency dependency = new Dependency("com.google.code.gson:gson:2.10.1");
        List<Dependency> list = dependency.getSubDependencies();
        DependencyDownloader.downloadAll(list, "./libs", 8);
        DependencyLoader loader = new DependencyLoader("./libs/", list); // Load all dependencies in the libs folder
        DependencyClass gson = loader.construct("com.google.gson.Gson"); // Declare a variable of type Gson
        Map<String, String> map = new HashMap<>(); // Declare a HashMap to store JSON data
        map.put("gson", "2.10.1");
        map.put("JavaDependencyManager", "1.2");
        map.put("log4j-core", "2.20.0");
        // Declare a variable of type JsonObject and assign it the parsed HashMap data
        DependencyClass jsonObject = new DependencyClass(gson.runMethod("fromJson", gson.runMethod("toJson", map), loader.getLocalClass("com.google.gson.JsonObject")));
        System.out.println(jsonObject.runMethod("get", "gson").toString()); // Call the method
        System.out.println(gson.runMethod("toJson", jsonObject.getObject()).toString()); // Same as above
    }
}
```

Here, we can use the `construct()` method of the `DependencyLoader` class to invoke the constructor of a class. In the code, we use `loader.construct("com.google.gson.Gson")` to call the constructor of the `Gson` class. This `construct()` method returns a variable of type `DependencyClass`, representing the constructed class.

Next, if we want to call a method of a class, we can use the `runMethod()` method of the `DependencyClass` class. The first parameter is the name of the method to be called, and the subsequent parameters are the arguments required for this method. (Note: The `runMethod()` method cannot be used to call static methods. For calling static methods, see below. The consequences of using `runMethod()` to call static methods are not the responsibility of the original author.)

##### Using Static Methods

For instance, if we want to use the log4j library to log information, the original code might look like this:

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

We can transform the code to:

```java
import java.util.List;

public class Test_Log4j2 {
    public static void main(String[] args) throws Exception {
        Dependency dependency = new Dependency("org.apache.logging.log4j:log4j-core:2.20.0");
        List<Dependency> list = dependency.getSubDependencies();
        DependencyDownloader.downloadAll(list, "./libs", 8);
        DependencyLoader loader = new DependencyLoader("./libs/", list);
        DependencyClass logger = new DependencyClass(loader.runStaticMethod("org.apache.logging.log4j.LogManager", "getLogger", Test.class));
        logger.runMethod("info", "Hello World!");
    }
}
```

Here, we can use the `runStaticMethod()` method of the `DependencyLoader` class to call a static method. The first parameter is the class where the static method is located, the second parameter is the method name, and the following parameters are the required arguments for the method.

#### JavaDoc

You can find the JavaDoc for this project [here](https://docs.alazeprt.com/).

### Issue Tracking

If you have questions, ideas, or you discover any bugs, you can submit them [here](https://github.com/alazeprt/JavaDependencyManager/issues).

### License

This project uses the GPL-3.0 license.