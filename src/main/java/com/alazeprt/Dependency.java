package com.alazeprt;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Dependency {
    private final String dependency;
    public Dependency(String dependency) {
        this.dependency = dependency;
    }

    private List<Dependency> getSubDependencies(List<Dependency> list) throws IOException {
        List<Dependency> dependencies = new ArrayList<>();
        for (Dependency dependency : list) {
            if(dependency.getDependency().contains("junit")) {
                continue;
            }
            InputStream in;
            if(dependency.isExternal()) {
                ExternalDependency external = (ExternalDependency) dependency;
                URL pomUrl = new URL(external.parseDependency() + "/" + dependency.getDependency().split(":")[1] + "-" + dependency.getDependency().split(":")[2] + ".pom");
                HttpURLConnection connection = (HttpURLConnection) pomUrl.openConnection();
                in = connection.getInputStream();
            } else {
                in = new FileInputStream(dependency.getDependency());
            }
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model;
            try {
                model = reader.read(in);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            in.close();
            if (model.getDependencies().isEmpty()) {
                dependencies.add(dependency);
            } else {
                List<Dependency> subDependencies = new ArrayList<>();
                for (org.apache.maven.model.Dependency mavenDependency : model.getDependencies()) {
                    if(mavenDependency.getScope() != null && mavenDependency.getScope().equals("test")) {
                        continue;
                    }
                    List<Dependency> list1 = new ArrayList<>();
                    list1.addAll(subDependencies);
                    list1.addAll(dependencies);
                    list1.addAll(list);
                    subDependencies.add(getDependencyInfo(mavenDependency, list1));
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

    private Dependency getDependencyInfo(org.apache.maven.model.Dependency mavenDependency, List<Dependency> list1) throws IOException {
        if(mavenDependency.getVersion() == null || mavenDependency.getVersion().startsWith("${")) {
            if(mavenDependency.getGroupId().startsWith("${")) {
                String version = getSameVersion(list1, mavenDependency.getArtifactId(), mavenDependency.getArtifactId());
                return new ExternalDependency(mavenDependency.getArtifactId(), mavenDependency.getArtifactId(), version);
            } else {
                String version = getSameVersion(list1, mavenDependency.getGroupId(), mavenDependency.getArtifactId());
                return new ExternalDependency(mavenDependency.getGroupId(), mavenDependency.getArtifactId(), version);
            }
        } else {
            if(mavenDependency.getGroupId().startsWith("${")) {
                String version = getSameVersion(list1, mavenDependency.getArtifactId(), mavenDependency.getArtifactId());
                return new ExternalDependency(mavenDependency.getArtifactId(), mavenDependency.getArtifactId(), version);
            } else {
                String version = getSameVersion(list1, mavenDependency.getGroupId(), mavenDependency.getArtifactId());
                return new ExternalDependency(mavenDependency.getGroupId(), mavenDependency.getArtifactId(), version);
            }
        }
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

    public abstract boolean isExternal();

    public String getDependency() {
        return dependency;
    }
}
