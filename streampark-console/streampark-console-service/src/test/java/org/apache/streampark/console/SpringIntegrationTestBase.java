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

package org.apache.streampark.console;

import org.apache.streampark.common.conf.CommonConfig;
import org.apache.streampark.common.conf.ConfigConst;
import org.apache.streampark.common.util.SystemPropertyUtils;

import org.apache.commons.io.FileUtils;
import org.apache.flink.util.Preconditions;
import org.apache.hadoop.io.IOUtils;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rauschig.jarchivelib.Archiver;
import org.rauschig.jarchivelib.ArchiverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Nonnull;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Integration base tester. Note: The all children classes of the base must run after the
 * project-level package phrase.
 */
@Slf4j
@EnableScheduling
@ActiveProfiles("integration-test")
@AutoConfigureTestEntityManager
@AutoConfigureWebTestClient(timeout = "60000")
@TestPropertySource(locations = {"classpath:application-integration-test.yml"})
@ExtendWith({MockitoExtension.class, SpringExtension.class})
@SpringBootTest(
    classes = StreamParkConsoleBootstrap.class,
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public abstract class SpringIntegrationTestBase {
  protected static final Logger LOG = LoggerFactory.getLogger(SpringIntegrationTestBase.class);

  protected static final String RUN_PKG_SCRIPT_HINT =
      "Please run package script before running the test case.";

  protected static final String DEFAULT_APP_HOME_DIR_NAME = "apache-streampark";
  protected static final String DEFAULT_FLINK_OFFICIAL_RELEASES_DIR_NAME =
      "flink-official-releases";
  protected static final String DEFAULT_LOCAL_WORKSPACE_DIR_NAME = "localWorkspace";
  protected static final String DEFAULT_FLINK_VERSION = "1.17.1";
  protected static final String DEFAULT_FLINK_DOWNLOAD_URL =
      "https://dlcdn.apache.org/flink/flink-1.17.1/flink-1.17.1-bin-scala_2.12.tgz";
  protected static final FileFilter PKG_NAME_FILTER =
      file -> file.getName().startsWith(DEFAULT_APP_HOME_DIR_NAME) && file.isDirectory();

  protected static String defaultFlinkHome;
  protected static String appHome;

  @BeforeAll
  public static void init(@TempDir File tempPath) throws IOException {

    LOG.info("Start prepare the real running env.");
    String tempAbsPath = tempPath.getAbsolutePath();
    LOG.info("Integration test base tmp dir: {}", tempAbsPath);

    FileUtils.copyDirectory(
        tryFindStreamParkPackagedDirFile(), new File(tempAbsPath, DEFAULT_APP_HOME_DIR_NAME));

    prepareDefaultFlinkHome(tempAbsPath, 3);
    Path localWorkspace =
        Files.createDirectories(new File(tempAbsPath, DEFAULT_LOCAL_WORKSPACE_DIR_NAME).toPath());

    appHome = new File(tempAbsPath, DEFAULT_APP_HOME_DIR_NAME).getAbsolutePath();
    System.setProperty(ConfigConst.KEY_APP_HOME(), appHome);
    System.setProperty(
        CommonConfig.STREAMPARK_WORKSPACE_LOCAL().key(),
        localWorkspace.toAbsolutePath().toString());

    LOG.info(
        "Complete mock EnvInitializer init, app home: {}, {}: {}",
        appHome,
        CommonConfig.STREAMPARK_WORKSPACE_LOCAL().key(),
        localWorkspace.toAbsolutePath());
  }

  private static void prepareDefaultFlinkHome(String tempAbsPath, int reties) throws IOException {
    reties--;
    if (reties < 0) {
      LOG.error("Failed to prepareDefaultFlinkHome with multiple-retires.");
      return;
    }
    try {
      final File flinkOfficialReleases =
          new File(tempAbsPath, DEFAULT_FLINK_OFFICIAL_RELEASES_DIR_NAME);
      Files.deleteIfExists(flinkOfficialReleases.toPath());
      Files.createDirectories(flinkOfficialReleases.toPath());

      defaultFlinkHome =
          prepareFlinkOfficialRelease(
              DEFAULT_FLINK_DOWNLOAD_URL,
              DEFAULT_FLINK_VERSION,
              flinkOfficialReleases.getAbsolutePath());
    } catch (Throwable t) {
      LOG.error("Error in prepareDefaultFlinkHome with exception", t);
      LOG.warn("Start the next retry.");
      prepareDefaultFlinkHome(tempAbsPath, reties);
    }
  }

  /**
   * @param httpUrl flink official release download url.
   * @return return the target un-packed flink home absolute dir.
   */
  private static String prepareFlinkOfficialRelease(
      @Nonnull String httpUrl, @Nonnull String flinkVersion, String workDirAbsolutePath)
      throws IOException {
    String downloadedFilePath =
        new File(workDirAbsolutePath, flinkVersion).getAbsolutePath() + ".tgz";
    httpDownload(httpUrl, downloadedFilePath);
    File archive = new File(downloadedFilePath);
    File destination = new File(archive.getParentFile().getAbsolutePath());
    Files.createDirectories(destination.toPath());

    Archiver archiver = ArchiverFactory.createArchiver("tar", "gz");
    archiver.extract(archive, destination);
    Optional<File> first =
        Arrays.stream(
                requireNonNull(
                    destination.listFiles(
                        file -> file.getName().contains(flinkVersion) && file.isDirectory())))
            .findFirst();
    File file =
        first.orElseThrow(
            () ->
                new RuntimeException(
                    String.format(
                        "Error in prepareFlinkOfficialRelease for httpUrl: %s, flinkVersion: %s",
                        httpUrl, flinkVersion)));

    LOG.info("Prepared flink release: {}.", file.getAbsolutePath());
    return file.getAbsolutePath();
  }

  private static void httpDownload(String httpUrl, String saveFile) {

    try {
      URL url = new URL(httpUrl);
      URLConnection conn = url.openConnection();
      InputStream inStream = conn.getInputStream();
      FileOutputStream fs = new FileOutputStream(saveFile);
      IOUtils.copyBytes(inStream, fs, 2048);
      inStream.close();
      fs.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static File tryFindStreamParkPackagedDirFile() {
    String userDir = Preconditions.checkNotNull(SystemPropertyUtils.get("user.dir"));
    File pkgTargetDirFile = new File(userDir, "target");
    Preconditions.checkState(
        pkgTargetDirFile.exists(),
        "The target directory of %s doesn't exist. %s",
        userDir,
        RUN_PKG_SCRIPT_HINT);
    Optional<File> availablePkgParentFileOpt =
        Arrays.stream(requireNonNull(pkgTargetDirFile.listFiles(PKG_NAME_FILTER))).findFirst();
    final File availablePkgParentFile =
        availablePkgParentFileOpt.orElseThrow(() -> new RuntimeException(RUN_PKG_SCRIPT_HINT));
    Optional<File> targetDirFile =
        Arrays.stream(requireNonNull(availablePkgParentFile.listFiles(PKG_NAME_FILTER)))
            .findFirst();
    return targetDirFile.orElseThrow(() -> new RuntimeException(RUN_PKG_SCRIPT_HINT));
  }
}
