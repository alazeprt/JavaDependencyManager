package com.alazeprt;

import java.io.File;

/**
 * Represents a Maven dependency and provides methods for retrieving sub-dependencies.
 * Also provides methods for parsing dependency information and obtaining URLs.
 *
 * @author alazeprt
 */
public class LocalDependency extends Dependency {
    private final File dependency;

    /**
     * Initializes a dependency with default central URL.
     *
     * @param dependency Shortening name of the dependency
     */
    public LocalDependency(String dependency) {
        super(dependency);
        this.dependency = new File(dependency);
    }

    /**
     * Gets the shortening name of the dependency.
     *
     * @return Dependency shortening name
     */
    public String getDependency() {
        return dependency.getAbsolutePath();
    }

    @Override
    public boolean isExternal() {
        return false;
    }
}
