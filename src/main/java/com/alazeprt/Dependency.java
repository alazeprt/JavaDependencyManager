package com.alazeprt;

public class Dependency {
    private final String dependency;
    private final String centralUrl;
    public Dependency(String dependency) {
        this.dependency = dependency;
        this.centralUrl = "https://repo.maven.apache.org/maven2/";
    }

    public Dependency(String dependency, String centralUrl) {
        this.dependency = dependency;
        if(!centralUrl.endsWith("/")) {
            centralUrl += "/";
        }
        this.centralUrl = centralUrl;
    }

    public String parseDependency() {
        String[] strings = dependency.split(":");
        String packageUrl = strings[0].replace(".", "/");
        return centralUrl + packageUrl + "/" + strings[1] + "/" + strings[2];
    }

    public String getPomContent() {
        String[] strings = dependency.split(":");
        String pomUrl = parseDependency() + "/" + strings[1] + "-" + strings[2] + ".pom";
    }

    public static void main(String[] args) {
        Dependency dependency = new Dependency("com.google.code.gson:gson:2.9.0");
        System.out.println(dependency.parseDependency());
    }
}