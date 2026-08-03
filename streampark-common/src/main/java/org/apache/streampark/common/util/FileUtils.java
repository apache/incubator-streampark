/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.streampark.common.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public final class FileUtils {

    private FileUtils() {
    }

    public static File toCanonicalFile(String path) throws IOException {
        Path normalized = Paths.get(path).normalize();
        if (normalized.toString().contains("..")) {
            throw new IOException("Invalid path: " + path);
        }
        return normalized.toFile().getCanonicalFile();
    }

    public static File resolveChildFile(File baseDir, String... childSegments) throws IOException {
        Path base = baseDir.getCanonicalFile().toPath();
        Path current = base;
        for (String segment : childSegments) {
            if (segment == null || segment.isEmpty() || segment.contains("..")) {
                throw new IOException("Invalid path segment: " + segment);
            }
            current = current.resolve(segment);
        }
        Path resolved = current.normalize().toAbsolutePath();
        if (!resolved.startsWith(base)) {
            throw new IOException("Resolved path escapes base directory: " + resolved);
        }
        return resolved.toFile();
    }

    private static String bytesToHexString(byte[] src) {
        if (src == null || src.length <= 0) {
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (byte b : src) {
            int v = b & 0xFF;
            String hv = Integer.toHexString(v).toUpperCase();
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    public static boolean isJarFileType(InputStream input) {
        if (input == null) {
            throw new RuntimeException("The inputStream can not be null");
        }
        return AutoCloseUtils.using(
            input,
            in -> {
                byte[] b = new byte[4];
                try {
                    in.read(b, 0, b.length);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return bytesToHexString(b);
            })
            .equals("504B0304");
    }

    public static boolean isJarFileType(File file) throws IOException {
        if (!file.exists() || !file.isFile()) {
            throw new RuntimeException("The file does not exist or the path is a directory");
        }
        return isJarFileType(new FileInputStream(file));
    }

    public static File createTempDir() {
        final int TEMP_DIR_ATTEMPTS = 10000;
        File baseDir = new File(System.getProperty("java.io.tmpdir"));
        String baseName = System.currentTimeMillis() + "-";
        for (int counter = 0; counter < TEMP_DIR_ATTEMPTS; counter++) {
            File tempDir = new File(baseDir, baseName + counter);
            if (tempDir.mkdir()) {
                return tempDir;
            }
        }
        throw new IllegalStateException(
            "[StreamPark] Failed to create directory within "
                + TEMP_DIR_ATTEMPTS
                + "  attempts (tried "
                + baseName
                + " 0 to "
                + baseName
                + (TEMP_DIR_ATTEMPTS - 1)
                + ")");
    }

    public static void mkdir(File dir) throws IOException {
        if (dir.exists() && !dir.isDirectory()) {
            throw new IOException("File " + dir + " exists and is not a directory. Unable to create directory.");
        } else if (!dir.mkdirs() && !dir.isDirectory()) {
            throw new IOException("Unable to create directory " + dir);
        }
    }

    public static String getPathFromEnv(String env) {
        String path = System.getenv(env);
        if (path == null) {
            path = System.getProperty(env);
        }
        AssertUtils.notNull(path, "[StreamPark] FileUtils.getPathFromEnv: " + env + " is not set on system env");
        File file = new File(path);
        if (!file.exists()) {
            throw new IllegalArgumentException("[StreamPark] FileUtils.getPathFromEnv: " + env + " is not exist!");
        }
        return file.getAbsolutePath();
    }

    public static String resolvePath(String parent, String child) {
        File file = new File(parent, child);
        if (!file.exists()) {
            throw new IllegalArgumentException(
                "[StreamPark] FileUtils.resolvePath: " + file.getAbsolutePath() + " is not exist!");
        }
        return file.getAbsolutePath();
    }

    public static String getSuffix(String filename) {
        AssertUtils.notNull(filename);
        return filename.substring(filename.lastIndexOf("."));
    }

    public static List<URL> listFileAsURL(String dirPath) {
        File dir = new File(dirPath);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null && files.length > 0) {
                List<URL> urls = new ArrayList<>();
                for (File f : files) {
                    try {
                        urls.add(f.toURI().toURL());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                return urls;
            }
        }
        return Collections.emptyList();
    }

    public static boolean exists(Serializable file) {
        if (file == null) {
            return false;
        }
        if (file instanceof File) {
            return ((File) file).exists();
        }
        return new File(file.toString()).exists();
    }

    public static boolean directoryNotBlank(Serializable file) {
        if (file == null) {
            return false;
        }
        File f = file instanceof File ? (File) file : new File(file.toString());
        String[] list = f.list();
        return f.isDirectory() && list != null && list.length > 0;
    }

    public static boolean equals(File file1, File file2) {
        try {
            return equalsInternal(file1, file2);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean equalsInternal(File file1, File file2) throws IOException {
        if (file1 == null || file2 == null) {
            return false;
        }
        if (!file1.exists() || !file2.exists()) {
            return false;
        }
        if (file1.getAbsolutePath().equals(file2.getAbsolutePath())) {
            return true;
        }
        BufferedInputStream first = new BufferedInputStream(new FileInputStream(file1));
        BufferedInputStream second = new BufferedInputStream(new FileInputStream(file2));
        if (first.available() != second.available()) {
            Utils.close(first, second);
            return false;
        }
        while (true) {
            int firRead = first.read();
            int secRead = second.read();
            if (firRead != secRead) {
                Utils.close(first, second);
                return false;
            }
            if (firRead == -1) {
                Utils.close(first, second);
                return true;
            }
        }
    }

    public static void readInputStream(InputStream in, byte[] array) throws IOException {
        try (InputStream input = in) {
            int toRead = array.length;
            int off = 0;
            while (toRead > 0) {
                int ret = input.read(array, off, toRead);
                if (ret < 0) {
                    throw new IOException("Bad inputStream, premature EOF");
                }
                toRead -= ret;
                off += ret;
            }
        }
    }

    public static String readFile(String filename) throws IOException {
        return SafePathUtils.readConfigFile(filename);
    }

    public static String readFile(File file) throws IOException {
        return readFile(file.getPath());
    }

    public static void writeFile(String content, File file) throws IOException {
        java.nio.channels.WritableByteChannel channel =
            Channels.newChannel(Files.newOutputStream(file.toPath()));
        ByteBuffer buffer = ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8));
        channel.write(buffer);
        Utils.close(channel);
    }

    public static byte[] readEndOfFile(File file, long maxSize) throws IOException {
        try (RandomAccessFile raFile = new RandomAccessFile(file, "r")) {
            long readSize = maxSize;
            if (raFile.length() > maxSize) {
                raFile.seek(raFile.length() - maxSize);
            } else if (raFile.length() < maxSize) {
                readSize = raFile.length();
            }
            byte[] fileContent = new byte[(int) readSize];
            raFile.read(fileContent);
            return fileContent;
        }
    }

    public static byte[] readFileFromOffset(File file, long startOffset, long maxSize) throws IOException {
        if (file.length() < startOffset) {
            throw new IllegalArgumentException(
                "The startOffset "
                    + startOffset
                    + " is great than the file length "
                    + file.length());
        }
        try (RandomAccessFile raFile = new RandomAccessFile(file, "r")) {
            long readSize = Math.min(maxSize, file.length() - startOffset);
            raFile.seek(startOffset);
            byte[] fileContent = new byte[(int) readSize];
            raFile.read(fileContent);
            return fileContent;
        }
    }

    public static String tailOf(String path, int offset, int limit) {
        try {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                return Files.lines(Paths.get(path)).skip(offset).limit(limit).collect(Collectors.joining("\r\n"));
            }
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String readString(File file) throws IOException {
        if (file == null || !file.isFile()) {
            throw new IllegalArgumentException("file must be a normal file");
        }
        FileReader reader = new FileReader(file);
        Scanner scanner = new Scanner(reader);
        StringBuilder buffer = new StringBuilder();
        if (scanner.hasNextLine()) {
            buffer.append(scanner.nextLine());
        }
        while (scanner.hasNextLine()) {
            buffer.append("\r\n");
            buffer.append(scanner.nextLine());
        }
        Utils.close(scanner, reader);
        return buffer.toString();
    }
}
