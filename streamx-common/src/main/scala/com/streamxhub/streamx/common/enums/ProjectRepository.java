/*
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.streamx.common.enums;


import java.io.Serializable;

/**
 * @author nestor
 */

public enum ProjectRepository implements Serializable {

    /**
     * repository git
     */
    GIT(1, "GitHub/GitLab"),
    /**
     * repository svn
     */
    SVN(2, "Subversion"),
    /**
     * repository jar
     */
    JAR(3, "Jar");

    private Integer regository;
    private String name;

    ProjectRepository(Integer regository, String name) {
        this.regository = regository;
        this.name = name;
    }

    public Integer getRegository() {
        return regository;
    }

    public void setRegository(Integer regository) {
        this.regository = regository;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static ProjectRepository of(Integer value) {
        for (ProjectRepository projectRepository : values()) {
            if (projectRepository.regository.equals(value)) {
                return projectRepository;
            }
        }
        return null;
    }

}
