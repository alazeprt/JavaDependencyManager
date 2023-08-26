package com.alazeprt;

/**
 * Represents a Maven dependency and provides methods for retrieving sub-dependencies.
 * Also provides methods for parsing dependency information and obtaining URLs.
 *
 * @author alazeprt
 */
public class ExternalDependency extends Dependency {

    /**
     * Initializes a dependency with default central URL.
     *
     * @param dependency Shortening name of the dependency
     */
    public ExternalDependency(String dependency) {
        super(dependency);
    }

    /**
     * Initializes a dependency using groupId, artifactId, and version.
     *
     * @param groupId    GroupId of the dependency
     * @param artifactId ArtifactId of the dependency
     * @param version    Version of the dependency
     */
    public ExternalDependency(String groupId, String artifactId, String version) {
        super(groupId + ":" + artifactId + ":" + version);
    }

    /**
     * Parses and constructs the URL of the dependency.
     *
     * @return URL where the dependency is located
     */
    public String parseDependency() {
        String[] strings = getDependency().split(":");
        String packageUrl = strings[0].replace(".", "/");
        String centralUrl = "https://repo.maven.apache.org/maven2/";
        return centralUrl + packageUrl + "/" + strings[1] + "/" + strings[2];
    }

    @Override
    public boolean isExternal() {
        return true;
    }

    public String getDependency() {
        return super.getDependency();
    }
}
