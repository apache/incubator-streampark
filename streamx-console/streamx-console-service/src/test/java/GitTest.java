/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.streamxhub.streamx.console.core.entity.Project;
import com.streamxhub.streamx.console.core.enums.GitAuthorizedError;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class GitTest {

    private Project project = new Project();

    @Before
    public void before() {
        project.setUrl("https://github.com/streamxhub/streamx-quickstart");
    }

    @Test
    public void getBranchs() {
        List<String> branches =  project.getAllBranches();
        branches.forEach(System.out::println);
    }

    @Test
    public void auth() {
        GitAuthorizedError error =  project.gitCheck();
        System.out.println(error);
    }

}
