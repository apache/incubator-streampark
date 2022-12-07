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

package org.apache.streampark.console.base.util;

import org.apache.streampark.console.core.entity.Project;
import org.apache.streampark.console.core.enums.GitProtocol;
import org.junit.jupiter.api.Test;

public class GitUtilsTest {

    @Test
    public void testClone() throws Exception {
        Project project = new Project();
        project.setUrl("https://github.com/streamxhub/streampark-quickstart.git");
        project.setGitProtocol(GitProtocol.HTTPS.getValue());
        project.setBranches("dev");
        project.setAppSource("/Users/benjobs/Desktop/test");
        project.cleanCloned();
        GitUtils.clone(project);
    }

}
