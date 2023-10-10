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

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.jar.JarFile;

public final class MavenWrapperHelper {

  private static final String WRAPPER_VERSION = "3.2.0";

  private static final boolean VERBOSE = Boolean.parseBoolean(System.getenv("MVNW_VERBOSE"));

  public static void main(String[] args) {
    String action = args[0].toLowerCase();
    String[] actionArgs = Arrays.copyOfRange(args, 1, args.length);
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
      case "verify":
        String wrapperJar = actionArgs[0];
        log("maven-wrapper file checking: " + wrapperJar);
        try (JarFile ignored = new JarFile(wrapperJar, true)) {
          System.exit(0);
        } catch (Exception e) {
          System.exit(1);
        }
      default:
        throw new UnsupportedOperationException("Unknown action \"" + action + "\".");
    }
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

  private static void log(String msg) {
    if (VERBOSE) {
      System.out.println(msg);
    }
  }
}
