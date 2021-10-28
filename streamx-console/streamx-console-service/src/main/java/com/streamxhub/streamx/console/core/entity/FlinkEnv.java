/*
 *  Copyright (c) 2019 The StreamX Project
 *
 * <p>Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.streamxhub.streamx.console.core.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import com.streamxhub.streamx.common.domain.FlinkVersion;
import com.streamxhub.streamx.common.util.DeflaterUtils;
import com.streamxhub.streamx.common.util.PropertiesUtils;
import lombok.Data;
import net.minidev.json.annotate.JsonIgnore;
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

    public String getLargeVersion() {
        return this.version.substring(0, this.version.lastIndexOf("."));
    }

    public String getVersionOfFirst() {
        return this.version.split("\\.")[0];
    }

    public String getVersionOfMiddle() {
        return this.version.split("\\.")[1];
    }

    public String getVersionOfLast() {
        return this.version.split("\\.")[2];
    }


}
