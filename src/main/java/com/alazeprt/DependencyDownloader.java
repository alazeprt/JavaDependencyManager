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

public class DependencyDownloader {
    private final String dependency;
    private final String centralUrl;
    public DependencyDownloader(String dependency) {
        this.dependency = dependency;
        this.centralUrl = "https://repo.maven.apache.org/maven2/";
    }

    public DependencyDownloader(String dependency, String centralUrl) {
        this.dependency = dependency;
        if(!centralUrl.endsWith("/")) {
            centralUrl += "/";
        }
        this.centralUrl = centralUrl;
    }

    public DependencyDownloader(String groupId, String artifactId, String version) {
        this.dependency = groupId + ":" + artifactId + ":" + version;
        this.centralUrl = "https://repo.maven.apache.org/maven2/";
    }

    private String parseDependency() {
        String[] strings = dependency.split(":");
        String packageUrl = strings[0].replace(".", "/");
        return centralUrl + packageUrl + "/" + strings[1] + "/" + strings[2];
    }

    public String getDependency() {
        return dependency;
    }

    public static List<DependencyDownloader> getDependencies(List<DependencyDownloader> list) throws IOException, XmlPullParserException {
        List<DependencyDownloader> dependencies = new ArrayList<>();
        for (DependencyDownloader dependencyDownloader1 : list) {
            if(dependencyDownloader1.getDependency().split(":")[1].equals("junit") || dependencyDownloader1.getDependency().split(":")[1].equals("junit-jupiter-api")) {
                continue;
            }
            URL pomUrl = new URL(dependencyDownloader1.parseDependency() + "/" + dependencyDownloader1.getDependency().split(":")[1] + "-" + dependencyDownloader1.getDependency().split(":")[2] + ".pom");
            HttpURLConnection connection = (HttpURLConnection) pomUrl.openConnection();
            InputStream in = connection.getInputStream();
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read(in);
            in.close();
            connection.disconnect();

            if (model.getDependencies().isEmpty()) {
                dependencies.add(dependencyDownloader1);
            } else {
                List<DependencyDownloader> subDependencies = new ArrayList<>();
                for (org.apache.maven.model.Dependency mavenDependency : model.getDependencies()) {
                    if(mavenDependency.getArtifactId().equals("junit") || mavenDependency.getArtifactId().equals("junit-jupiter-api")) {
                        continue;
                    }
                    DependencyDownloader subDependencyDownloader;
                    List<DependencyDownloader> list1 = new ArrayList<>();
                    list1.addAll(subDependencies);
                    list1.addAll(dependencies);
                    list1.addAll(list);
                    if(mavenDependency.getVersion() == null || mavenDependency.getVersion().startsWith("${")) {
                        if(mavenDependency.getGroupId().startsWith("${")) {
                            String version = getSameVersion(list1, mavenDependency.getArtifactId(), mavenDependency.getArtifactId());
                            subDependencyDownloader = new DependencyDownloader(mavenDependency.getArtifactId(), mavenDependency.getArtifactId(), version);
                        } else {
                            String version = getSameVersion(list1, mavenDependency.getGroupId(), mavenDependency.getArtifactId());
                            subDependencyDownloader = new DependencyDownloader(mavenDependency.getGroupId(), mavenDependency.getArtifactId(), version);
                        }
                    } else {
                        if(mavenDependency.getGroupId().startsWith("${")) {
                            String version = getSameVersion(list1, mavenDependency.getArtifactId(), mavenDependency.getArtifactId());
                            subDependencyDownloader = new DependencyDownloader(mavenDependency.getArtifactId(), mavenDependency.getArtifactId(), version);
                        } else {
                            String version = getSameVersion(list1, mavenDependency.getGroupId(), mavenDependency.getArtifactId());
                            subDependencyDownloader = new DependencyDownloader(mavenDependency.getGroupId(), mavenDependency.getArtifactId(), version);
                        }
                    }
                    subDependencies.add(subDependencyDownloader);
                }
                dependencies.addAll(subDependencies);
            }
        }
        dependencies.addAll(list);
        return dependencies;
    }

    private static String getSameVersion(List<DependencyDownloader> subDependencies, String groupId, String artifactId) throws IOException {
        String version = getLatestVersion(groupId, artifactId);
        for(DependencyDownloader dependencyDownloader2 : subDependencies) {
            if(dependencyDownloader2.getDependency().split(":")[0].equals(groupId)) {
                if(versionFound(groupId, artifactId, dependencyDownloader2.getDependency().split(":")[2])) {
                    version = dependencyDownloader2.getDependency().split(":")[2];
                    break;
                }
            }
        }
        return version;
    }

    public static void downloadAll(List<DependencyDownloader> list, String outputFolder, int threads) throws IOException {
        File folder = new File(outputFolder);
        if(!folder.exists()) {
            folder.mkdirs();
        }
        if(!outputFolder.endsWith("/")) {
            outputFolder += "/";
        }
        for(DependencyDownloader dependencyDownloader1 : list) {
            String fileUrl = dependencyDownloader1.parseDependency();
            fileUrl += "/" + dependencyDownloader1.getDependency().split(":")[1] + "-" + dependencyDownloader1.getDependency().split(":")[2] + ".jar";
            System.out.println("Downloading " + dependencyDownloader1.getDependency().split(":")[1] + "-" + dependencyDownloader1.getDependency().split(":")[2] + ".jar");
            downloadFiles(fileUrl, outputFolder + dependencyDownloader1.getDependency().split(":")[1] + "-" + dependencyDownloader1.getDependency().split(":")[2] + ".jar", threads);
        }
    }

    private static void downloadFiles(String fileUrl, String outputFilePath, int numThreads) throws IOException {
        URL url = new URL(fileUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        int fileSize = connection.getContentLength();
        connection.disconnect();

        int chunkSize = fileSize / numThreads;

        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            int startByte = i * chunkSize;
            int endByte = (i == numThreads - 1) ? fileSize - 1 : (i + 1) * chunkSize - 1;
            threads[i] = new Thread(new DownloadThread(fileUrl, startByte, endByte, outputFilePath));
            threads[i].start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static String getLatestVersion(String groupId, String artifactId) throws IOException {
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

        // Use regex to find latest version
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

    private static boolean versionFound(String groupId, String artifactId, String version) throws IOException {
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
            if(response.toString().contains(version)) {
                return true;
            } else {
                return false;
            }
        }
    }

    private static class DownloadThread implements Runnable {
        private final String fileUrl;
        private final int startByte;
        private final int endByte;
        private final String outputFilePath;

        private DownloadThread(String fileUrl, int startByte, int endByte, String outputFilePath) {
            this.fileUrl = fileUrl;
            this.startByte = startByte;
            this.endByte = endByte;
            this.outputFilePath = outputFilePath;
        }

        @Override
        public void run() {
            try {
                URL url = new URL(fileUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("Range", "bytes=" + startByte + "-" + endByte);

                try (InputStream in = connection.getInputStream();
                     RandomAccessFile raf = new RandomAccessFile(outputFilePath, "rw")) {
                    raf.seek(startByte);
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        raf.write(buffer, 0, bytesRead);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        List<DependencyDownloader> list = new ArrayList<>();
        list.add(new DependencyDownloader("org.apache.logging.log4j:log4j-core:2.20.0"));
        list.addAll(getDependencies(list));
        for(DependencyDownloader d : list) {
            System.out.println(d.getDependency());
        }
        downloadAll(list, "output", 8);
    }
}