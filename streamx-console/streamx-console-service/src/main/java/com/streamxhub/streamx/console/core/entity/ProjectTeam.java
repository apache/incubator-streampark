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

package com.streamxhub.streamx.console.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("t_flink_projectTeam")
public class ProjectTeam {

    @TableId(value = "id", type = IdType.AUTO)
    private int id;

    @TableField("user_id")
    private int userId;

    @TableField(exist = false)
    private String userName;

    private long code;

    private String name;

    private String description;

    private Date createTime;

    private Date updateTime;

    public long getCode() {
        return code;
    }

    public void setCode(long code) {
        this.code = code;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public String toString() {
        return "Project{"
            + "id=" + id
            + ", userId=" + userId
            + ", userName='" + userName + '\''
            + ", code=" + code
            + ", name='" + name + '\''
            + ", description='" + description + '\''
            + ", createTime=" + createTime
            + ", updateTime=" + updateTime
            + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ProjectTeam projectTeam = (ProjectTeam) o;

        if (id != projectTeam.id) {
            return false;
        }
        return name.equals(projectTeam.name);

    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + name.hashCode();
        return result;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private int id;
        private int userId;
        private String userName;
        private long code;
        private String name;
        private String description;
        private Date createTime;
        private Date updateTime;

        private Builder() {
        }

        public Builder code(long code) {
            this.code = code;
            return this;
        }

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        public Builder userId(int userId) {
            this.userId = userId;
            return this;
        }

        public Builder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder createTime(Date createTime) {
            this.createTime = createTime;
            return this;
        }

        public Builder updateTime(Date updateTime) {
            this.updateTime = updateTime;
            return this;
        }

        public ProjectTeam build() {
            ProjectTeam projectTeam = new ProjectTeam();
            projectTeam.setId(id);
            projectTeam.setUserId(userId);
            projectTeam.setCode(code);
            projectTeam.setUserName(userName);
            projectTeam.setName(name);
            projectTeam.setDescription(description);
            projectTeam.setCreateTime(createTime);
            projectTeam.setUpdateTime(updateTime);
            return projectTeam;
        }
    }
}
