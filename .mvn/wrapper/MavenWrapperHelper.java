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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

public final class MavenWrapperHelper {

  public static final String PROJECT_STRING = "PROJECT";

  private static final String WRAPPER_VERSION = "3.2.0";

  public static final String DISTRIBUTION_BASE_PROPERTY = "distributionBase";

  public static final String DISTRIBUTION_PATH_PROPERTY = "distributionPath";

  public static final String MAVEN_USER_HOME_ENV_KEY = "MAVEN_USER_HOME";

  public static final String MAVEN_USER_HOME_STRING = "MAVEN_USER_HOME";

  public static final String MAVEN_USER_HOME_PROPERTY_KEY = "maven.user.home";

  public static final String ZIP_STORE_BASE_PROPERTY = "zipStoreBase";

  public static final String ZIP_STORE_PATH_PROPERTY = "zipStorePath";

  public static final Path DEFAULT_DISTRIBUTION_PATH = Paths.get("wrapper", "dists");

  private static final Path DEFAULT_MAVEN_USER_HOME =
      Paths.get(System.getProperty("user.home")).resolve(".m2");

  private static final boolean VERBOSE = Boolean.parseBoolean(System.getenv("MVNW_VERBOSE"));

  public static void main(String[] args) throws Exception {
    String action = args[0].toLowerCase();
    String[] actionArgs = Arrays.copyOfRange(args, 1, args.length);
    Properties properties;

    switch (action) {
      case "download":
        log("Apache Maven Wrapper Downloader " + WRAPPER_VERSION);
        if (actionArgs.length != 2) {
          System.err.println(" - ERROR wrapperUrl or wrapperJarPath parameter missing");
          System.exit(1);
        }
        try {
          log(" - Downloader started");
          final URL wrapperUrl = new URL(actionArgs[0]);
          final String jarPath = actionArgs[1].replace("src/main", ""); // Sanitize path
          final Path wrapperJarPath = Paths.get(jarPath).toAbsolutePath().normalize();
          downloadFileFromURL(wrapperUrl, wrapperJarPath);
          log("Done");
        } catch (IOException e) {
          System.err.println("- Error downloading: " + e.getMessage());
          if (VERBOSE) {
            e.printStackTrace();
          }
          System.exit(1);
        }
        break;

      case "verify_wrapper":
        String wrapperJar = actionArgs[0];
        properties = getProperties(actionArgs[1]);
        String wrapperMd5 = properties.getProperty("wrapperMd5");
        if (wrapperMd5 != null) {
          String fileMd5 = getFileMd5(wrapperJar);
          if (!wrapperMd5.equals(fileMd5)) {
            System.exit(1);
          }
        } else {
          try (JarFile ignored = new JarFile(wrapperJar, true)) {
          } catch (Exception e) {
            System.exit(1);
          }
        }
        break;

      case "verify_dist":
        properties = getProperties(actionArgs[0]);
        LocalDistribution distribution = getLocalDistribution(properties);

        File zipFile = distribution.getZipFile();
        File unzipFile = distribution.getDistributionDir();
        if (unzipFile.exists()) {
          // 1) check distribution unzip files
          String cmd = getMavenCheckCMD(actionArgs);
          boolean success;
          try {
            Process process = Runtime.getRuntime().exec(cmd);
            process.waitFor();
            success = process.exitValue() == 0;
          } catch (Exception e) {
            success = false;
          }
          if (!success) {
            deleteDir(unzipFile);
            if (unzipFile.exists()) {
              System.out.println(unzipFile.getAbsolutePath());
              System.exit(1);
            }
          }
        } else if (zipFile.exists()) {
          // 2) check distribution zip file
          String distributionPath = zipFile.getAbsolutePath();
          String distributionMd5 = properties.getProperty("distributionMd5");
          if (distributionMd5 != null) {
            String fileMd5 = getFileMd5(distributionPath);
            if (!distributionMd5.equals(fileMd5)) {
              boolean success = deleteDistribution(distribution);
              if (!success) {
                System.out.println(zipFile.getAbsolutePath());
                System.exit(1);
              }
            }
          } else {
            try (ZipFile ignored = new ZipFile(zipFile)) {
            } catch (Exception e) {
              boolean success = deleteDistribution(distribution);
              if (!success) {
                System.out.println(zipFile.getAbsolutePath());
                System.exit(1);
              }
            }
          }
        }
        break;
      default:
        System.out.println("Unknown action");
        System.exit(2);
    }
  }

  private static Properties getProperties(String path) throws IOException {
    Properties properties = new Properties();
    properties.load(Files.newInputStream(new File(path).toPath()));
    return properties;
  }

  private static String getMavenCheckCMD(String[] actionArgs) {
    String javaCMD = actionArgs[1];
    String mvnWrapperHome = actionArgs[2];
    String format =
        "%s -classpath %s/.mvn/wrapper/maven-wrapper.jar "
            + "-Dmaven.multiModuleProjectDirectory=%s "
            + "org.apache.maven.wrapper.MavenWrapperMain -h";
    return String.format(format, javaCMD, mvnWrapperHome, mvnWrapperHome);
  }

  private static boolean deleteDistribution(LocalDistribution distribution) {
    String distPath = distribution.getZipFile().getAbsolutePath();
    System.err.println("- check distribution error: " + distPath + "is invalid");
    try {
      return distribution.getZipFile().delete();
    } catch (Exception e) {
      System.err.println("- delete distribution error: " + distPath);
      return false;
    }
  }

  private static LocalDistribution getLocalDistribution(Properties properties)
      throws URISyntaxException {
    String distributionUrl = properties.getProperty("distributionUrl");
    URI uri = new URI(distributionUrl);
    String baseName = getBaseName(uri);
    String distName = removeExtension(baseName);
    Path rootDirName = rootDirName(distName, uri);

    String distributionBase =
        properties.getProperty(DISTRIBUTION_BASE_PROPERTY, MAVEN_USER_HOME_STRING);

    Path distributionPath =
        Paths.get(
            properties.getProperty(
                DISTRIBUTION_PATH_PROPERTY, DEFAULT_DISTRIBUTION_PATH.toString()));

    String zipBase = properties.getProperty(ZIP_STORE_BASE_PROPERTY, MAVEN_USER_HOME_STRING);

    Path zipPath =
        Paths.get(
            properties.getProperty(ZIP_STORE_PATH_PROPERTY, DEFAULT_DISTRIBUTION_PATH.toString()));

    Path distDir = getBaseDir(distributionBase).resolve(distributionPath).resolve(rootDirName);

    Path distZip = getBaseDir(zipBase).resolve(zipPath).resolve(rootDirName).resolve(baseName);

    return new LocalDistribution(distDir.toFile(), distZip.toFile());
  }

  private static void downloadFileFromURL(URL wrapperUrl, Path wrapperJarPath) throws IOException {
    log(" - Downloading to: " + wrapperJarPath);
    if (System.getenv("MVNW_USERNAME") != null && System.getenv("MVNW_PASSWORD") != null) {
      final String username = System.getenv("MVNW_USERNAME");
      final char[] password = System.getenv("MVNW_PASSWORD").toCharArray();
      Authenticator.setDefault(
          new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
              return new PasswordAuthentication(username, password);
            }
          });
    }
    try (InputStream inStream = wrapperUrl.openStream()) {
      Files.copy(inStream, wrapperJarPath, StandardCopyOption.REPLACE_EXISTING);
    }
    log(" - Downloader complete");
  }

  private static String getFileMd5(String path) throws Exception {
    MessageDigest md5 = MessageDigest.getInstance("MD5");
    try (FileInputStream inputStream = new FileInputStream(path)) {
      byte[] buffer = new byte[1024];
      int len;
      while ((len = inputStream.read(buffer)) != -1) {
        md5.update(buffer, 0, len);
      }
      byte[] byteArray = md5.digest();
      StringBuilder sb = new StringBuilder();
      for (byte b : byteArray) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    }
  }

  private static void log(String msg) {
    if (VERBOSE) {
      System.out.println(msg);
    }
  }

  private static String getBaseName(URI uri) {
    return Paths.get(uri.getPath()).getFileName().toString();
  }

  private static String removeExtension(String name) {
    int dot = name.lastIndexOf(".");
    return dot > 0 ? name.substring(0, dot) : name;
  }

  private static Path rootDirName(String distName, URI uri) {
    String urlHash = getHash(uri);
    return Paths.get(distName, urlHash);
  }

  private static String getHash(URI path) {
    return Integer.toHexString(path.hashCode());
  }

  private static Path getBaseDir(String base) {
    if (MAVEN_USER_HOME_STRING.equals(base)) {
      return mavenUserHome();
    } else if (PROJECT_STRING.equals(base)) {
      return Paths.get(System.getProperty("user.dir"));
    } else {
      throw new RuntimeException("Base: " + base + " is unknown");
    }
  }

  private static Path mavenUserHome() {
    String mavenUserHome = System.getProperty(MAVEN_USER_HOME_PROPERTY_KEY);
    if (mavenUserHome == null) {
      mavenUserHome = System.getenv(MAVEN_USER_HOME_ENV_KEY);
    }
    return mavenUserHome == null ? DEFAULT_MAVEN_USER_HOME : Paths.get(mavenUserHome);
  }

  private static void deleteDir(File directory) {
    if (!directory.exists()) {
      return;
    }
    File[] files = directory.listFiles();
    if (files != null) {
      for (File file : files) {
        if (file.isDirectory()) {
          deleteDir(file);
        } else {
          file.delete();
        }
      }
    }
    directory.delete();
  }

  public static class LocalDistribution {
    private final File distZip;

    private final File distDir;

    public LocalDistribution(File distDir, File distZip) {
      this.distDir = distDir;
      this.distZip = distZip;
    }

    public File getDistributionDir() {
      return distDir;
    }

    public File getZipFile() {
      return distZip;
    }
  }
}