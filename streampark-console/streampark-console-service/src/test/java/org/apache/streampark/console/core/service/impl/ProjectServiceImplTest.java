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

package org.apache.streampark.console.core.service.impl;

import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.core.entity.Project;
import org.apache.streampark.console.core.mapper.ProjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ProjectServiceImpl} module-name validation on the {@code jars}, {@code
 * listConf} and {@code getAppConfPath} methods.
 */
@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

  @Mock private ProjectMapper projectMapper;

  private final ProjectServiceImpl projectService = new ProjectServiceImpl();

  @TempDir private Path tempDir;

  @BeforeEach
  void setUp() {
    // ServiceImpl#getById delegates to baseMapper.selectById(...).
    ReflectionTestUtils.setField(projectService, "baseMapper", projectMapper);
  }

  @Test
  void jarsShouldListJarFilesUnderProjectModule() throws Exception {
    Path distHome = Files.createDirectory(tempDir.resolve("dist"));
    Path moduleDir = Files.createDirectory(distHome.resolve("app"));
    Files.createFile(moduleDir.resolve("foo.jar"));
    Files.createFile(moduleDir.resolve("README.txt"));

    List<String> jars = projectService.jars(project(1L, distHome, "app"));

    // Only direct children ending with .jar are returned.
    assertThat(jars).containsExactly("foo.jar");
  }

  @Test
  void jarsShouldRejectRelativeModule() throws Exception {
    Files.createDirectories(tempDir.resolve("outside"));

    assertThatThrownBy(() -> projectService.jars(project(1L, tempDir, "../outside")))
        .isInstanceOf(ApiAlertException.class)
        .hasMessage("Invalid module.");
  }

  @Test
  void jarsShouldRejectModuleWithPathSeparator() throws Exception {
    assertThatThrownBy(() -> projectService.jars(project(1L, tempDir, "a/b")))
        .isInstanceOf(ApiAlertException.class)
        .hasMessage("Invalid module.");
  }

  @Test
  void listConfShouldRejectRelativeModule() {
    assertThatThrownBy(() -> projectService.listConf(project(1L, tempDir, "../../etc")))
        .isInstanceOf(ApiAlertException.class)
        .hasMessage("Invalid module.");
  }

  @Test
  void getAppConfPathShouldRejectModuleWithPathSeparator() {
    assertThatThrownBy(() -> projectService.getAppConfPath(1L, "../etc"))
        .isInstanceOf(ApiAlertException.class)
        .hasMessage("Invalid module.");
  }

  @Test
  void getAppConfPathShouldRejectBlankModule() {
    assertThatThrownBy(() -> projectService.getAppConfPath(1L, " "))
        .isInstanceOf(ApiAlertException.class)
        .hasMessage("Invalid module.");
  }

  @Test
  void getAppConfPathShouldResolveValidModule() throws Exception {
    Path distHome = Files.createDirectory(tempDir.resolve("dist"));
    Files.createDirectories(distHome.resolve("app"));
    when(projectMapper.selectById(1L)).thenReturn(project(1L, distHome, null));

    String confPath = projectService.getAppConfPath(1L, "app");

    assertThat(confPath).isEqualTo(distHome.resolve("app").toFile().getAbsolutePath());
  }

  private Project project(Long id, Path distHome, String module) {
    return new Project() {
      {
        setId(id);
        if (module != null) {
          setModule(module);
        }
      }

      @Override
      public File getDistHome() {
        return distHome.toFile();
      }
    };
  }
}
