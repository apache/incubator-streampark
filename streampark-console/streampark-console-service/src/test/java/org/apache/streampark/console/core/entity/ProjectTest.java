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

package org.apache.streampark.console.core.entity;

import org.apache.streampark.console.core.enums.GitAuthorizedErrorEnum;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

@Slf4j
class ProjectTest {

    private final Project project = new Project();

    @BeforeEach
    void before() {
        project.setUrl("https://github.com/apache/incubator-streampark.git");
    }

    @Disabled("This test case can't be runnable due to external service is not available.")
    @Test
    void testGitBranches() {
        List<String> branches = project.getAllBranches();
        branches.forEach(System.out::println);
    }

    @Disabled("This test case can't be runnable due to external service is not available.")
    @Test
    void testGitCheckAuth() {
        GitAuthorizedErrorEnum error = project.gitCheck();
        log.error("{}", error);
    }
}
