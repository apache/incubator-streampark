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

import com.streamxhub.streamx.common.domain.FlinkVersion;
import com.streamxhub.streamx.common.util.DeflaterUtils;
import com.streamxhub.streamx.common.util.PropertiesUtils;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * @author benjobs
 */
@Data
@TableName("t_flink_env")
public class FlinkEnv implements Serializable {

    private Long id;

    private String flinkName;

    private String flinkHome;

    private String flinkConf;

    private String description;

    private String scalaVersion;

    private String version;

    /**
     * 是否为默认版本.
     */
    private Boolean isDefault;

    private Date createTime;

    private transient FlinkVersion flinkVersion;

    private transient String streamxScalaVersion = scala.util.Properties.versionNumberString();

    public void doSetFlinkConf() throws IOException {
        assert this.flinkHome != null;
        File yaml = new File(this.flinkHome.concat("/conf/flink-conf.yaml"));
        assert yaml.exists();
        String flinkConf = FileUtils.readFileToString(yaml);
        this.flinkConf = DeflaterUtils.zipString(flinkConf);
    }

    public void doSetVersion() {
        assert this.flinkHome != null;
        this.setVersion(this.getFlinkVersion().version());
        this.setScalaVersion(this.getFlinkVersion().scalaVersion());
        if (!streamxScalaVersion.startsWith(this.getFlinkVersion().scalaVersion())) {
            throw new UnsupportedOperationException(
                String.format(
                    "The current Scala version of StreamX is %s, but the scala version of Flink to be added is %s, which does not match, Please check",
                    streamxScalaVersion,
                    this.getFlinkVersion().scalaVersion()
                )
            );
        }
    }

    @JsonIgnore
    public Map<String, String> convertFlinkYamlAsMap() {
        String flinkYamlString = DeflaterUtils.unzipString(flinkConf);
        return PropertiesUtils.loadFlinkConfYaml(flinkYamlString);
    }

    @JsonIgnore
    public FlinkVersion getFlinkVersion() {
        if (this.flinkVersion == null) {
            this.flinkVersion = new FlinkVersion(this.flinkHome);
        }
        return this.flinkVersion;
    }

    public void unzipFlinkConf() {
        this.flinkConf = DeflaterUtils.unzipString(this.flinkConf);
    }

    @JsonIgnore
    public String getLargeVersion() {
        return this.version.substring(0, this.version.lastIndexOf("."));
    }

    @JsonIgnore
    public String getVersionOfFirst() {
        return this.version.split("\\.")[0];
    }

    @JsonIgnore
    public String getVersionOfMiddle() {
        return this.version.split("\\.")[1];
    }

    @JsonIgnore
    public String getVersionOfLast() {
        return this.version.split("\\.")[2];
    }

}
