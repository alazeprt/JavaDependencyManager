package com.alazeprt;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * @author alazeprt
 * Dependency Downloader class
 */
public class DependencyDownloader {
    /** Download all dependencies in the list to the specified folder based on the specified number of threads
     * @param list All dependencies that need to be downloaded
     * @param outputFolder Location of dependency downloads
     * @param threads How many threads do each dependency need to be downloaded in
     * @throws IOException When unable to connect to the download link of the dependency
     */
    public static void downloadAll(List<Dependency> list, String outputFolder, int threads) throws IOException {
        File folder = new File(outputFolder);
        if(!folder.exists()) {
            folder.mkdirs();
        }
        if(!outputFolder.endsWith("/")) {
            outputFolder += "/";
        }
        for(Dependency dependency : list) {
            if(new File(outputFolder + dependency.getDependency().split(":")[1] + "-" + dependency.getDependency().split(":")[2] + ".jar").exists()) {
                continue;
            }
            String fileUrl = dependency.parseDependency();
            fileUrl += "/" + dependency.getDependency().split(":")[1] + "-" + dependency.getDependency().split(":")[2] + ".jar";
            System.out.println("Downloading " + dependency.getDependency().split(":")[1] + "-" + dependency.getDependency().split(":")[2] + ".jar");
            downloadFiles(fileUrl, outputFolder + dependency.getDependency().split(":")[1] + "-" + dependency.getDependency().split(":")[2] + ".jar", threads);
        }
    }

    /** Download the specified dependencies to the specified folder based on the specified number of threads */
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



    /** Multi threaded download class */
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
}