package com.alazeprt;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a Maven dependency and provides methods for retrieving sub-dependencies.
 * Also provides methods for parsing dependency information and obtaining URLs.
 *
 * @author alazeprt
 */
public class Dependency {
    private final String dependency;
    private final String centralUrl;

    /**
     * Initializes a dependency with default central URL.
     *
     * @param dependency Shortening name of the dependency
     */
    public Dependency(String dependency) {
        this.dependency = dependency;
        this.centralUrl = "https://repo.maven.apache.org/maven2/";
    }

    /**
     * Initializes a dependency with a custom central URL.
     *
     * @param dependency Shortening name of the dependency
     * @param centralUrl Link to the central repository where the dependency is located
     */
    public Dependency(String dependency, String centralUrl) {
        this.dependency = dependency;
        if (!centralUrl.endsWith("/")) {
            centralUrl += "/";
        }
        this.centralUrl = centralUrl;
    }

    /**
     * Initializes a dependency using groupId, artifactId, and version.
     *
     * @param groupId    GroupId of the dependency
     * @param artifactId ArtifactId of the dependency
     * @param version    Version of the dependency
     */
    public Dependency(String groupId, String artifactId, String version) {
        this.dependency = groupId + ":" + artifactId + ":" + version;
        this.centralUrl = "https://repo.maven.apache.org/maven2/";
    }

    /**
     * Gets the shortening name of the dependency.
     *
     * @return Dependency shortening name
     */
    public String getDependency() {
        return dependency;
    }

    /**
     * Parses and constructs the URL of the dependency.
     *
     * @return URL where the dependency is located
     */
    public String parseDependency() {
        String[] strings = dependency.split(":");
        String packageUrl = strings[0].replace(".", "/");
        return centralUrl + packageUrl + "/" + strings[1] + "/" + strings[2];
    }

    private List<Dependency> getSubDependencies(List<Dependency> list) throws IOException, XmlPullParserException {
        List<Dependency> dependencies = new ArrayList<>();
        for (Dependency dependency : list) {
            if(dependency.getDependency().split(":")[1].equals("junit") || dependency.getDependency().split(":")[1].equals("junit-jupiter-api")) {
                continue;
            }
            URL pomUrl = new URL(dependency.parseDependency() + "/" + dependency.getDependency().split(":")[1] + "-" + dependency.getDependency().split(":")[2] + ".pom");
            HttpURLConnection connection = (HttpURLConnection) pomUrl.openConnection();
            InputStream in = connection.getInputStream();
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read(in);
            in.close();
            connection.disconnect();

            if (model.getDependencies().isEmpty()) {
                dependencies.add(dependency);
            } else {
                List<Dependency> subDependencies = new ArrayList<>();
                for (org.apache.maven.model.Dependency mavenDependency : model.getDependencies()) {
                    if(mavenDependency.getArtifactId().equals("junit") || mavenDependency.getArtifactId().equals("junit-jupiter-api")) {
                        continue;
                    }
                    Dependency subDependency;
                    List<Dependency> list1 = new ArrayList<>();
                    list1.addAll(subDependencies);
                    list1.addAll(dependencies);
                    list1.addAll(list);
                    if(mavenDependency.getVersion() == null || mavenDependency.getVersion().startsWith("${")) {
                        if(mavenDependency.getGroupId().startsWith("${")) {
                            String version = getSameVersion(list1, mavenDependency.getArtifactId(), mavenDependency.getArtifactId());
                            subDependency = new Dependency(mavenDependency.getArtifactId(), mavenDependency.getArtifactId(), version);
                        } else {
                            String version = getSameVersion(list1, mavenDependency.getGroupId(), mavenDependency.getArtifactId());
                            subDependency = new Dependency(mavenDependency.getGroupId(), mavenDependency.getArtifactId(), version);
                        }
                    } else {
                        if(mavenDependency.getGroupId().startsWith("${")) {
                            String version = getSameVersion(list1, mavenDependency.getArtifactId(), mavenDependency.getArtifactId());
                            subDependency = new Dependency(mavenDependency.getArtifactId(), mavenDependency.getArtifactId(), version);
                        } else {
                            String version = getSameVersion(list1, mavenDependency.getGroupId(), mavenDependency.getArtifactId());
                            subDependency = new Dependency(mavenDependency.getGroupId(), mavenDependency.getArtifactId(), version);
                        }
                    }
                    subDependencies.add(subDependency);
                }
                dependencies.addAll(subDependencies);
            }
        }
        dependencies.addAll(list);
        return dependencies;
    }

    /**
     * Recursively retrieves all sub-dependencies of this dependency.
     *
     * @return List of all sub-dependencies that need to be downloaded (including itself)
     * @throws IOException            If unable to connect to the dependency's URL
     * @throws XmlPullParserException If the XML file of the dependency cannot be parsed
     */
    public List<Dependency> getSubDependencies() throws XmlPullParserException, IOException {
        List<Dependency> list = new ArrayList<>();
        list.add(this);
        return getSubDependencies(list);
    }

    private String getSameVersion(List<Dependency> subDependencies, String groupId, String artifactId) throws IOException {
        String version = getLatestVersion(groupId, artifactId);
        for(Dependency dependency : subDependencies) {
            if(dependency.getDependency().split(":")[0].equals(groupId)) {
                if(versionFound(groupId, artifactId, dependency.getDependency().split(":")[2])) {
                    version = dependency.getDependency().split(":")[2];
                    break;
                }
            }
        }
        return version;
    }

    private String getLatestVersion(String groupId, String artifactId) throws IOException {
        String mavenMetadataUrl = "https://repo.maven.apache.org/maven2/" +
                groupId.replace(".", "/") + "/" +
                artifactId + "/maven-metadata.xml";

        URL url = new URL(mavenMetadataUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        Pattern pattern = Pattern.compile("<latest>(.*?)</latest>");
        Matcher matcher = pattern.matcher(response.toString());
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            Pattern pattern1 = Pattern.compile("<version>(.*?)</version>");
            Matcher matcher1 = pattern1.matcher(response.toString());
            if(matcher1.find()) {
                return matcher1.group(1);
            } else {
                return null;
            }
        }
    }

    private boolean versionFound(String groupId, String artifactId, String version) throws IOException {
        String mavenMetadataUrl = "https://repo.maven.apache.org/maven2/" +
                groupId.replace(".", "/") + "/" +
                artifactId + "/maven-metadata.xml";

        URL url = new URL(mavenMetadataUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        if(response.isEmpty()) {
            return false;
        } else {
            return response.toString().contains(version);
        }
    }
}
