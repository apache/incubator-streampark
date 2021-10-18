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
import com.streamxhub.streamx.common.util.CommandUtils;
import com.streamxhub.streamx.common.util.DeflaterUtils;
import com.streamxhub.streamx.common.util.PropertiesUtils;
import lombok.Data;
import net.minidev.json.annotate.JsonIgnore;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author benjobs
 */
@Data
@TableName("t_flink_version")
public class FlinkVersion implements Serializable {

    private Long id;

    private String flinkName;

    private String flinkHome;

    private String flinkConf;

    private String description;

    private String version;

    /**
     * 是否为默认版本.
     */
    private Boolean isDefault;

    private Date createTime;

    @JsonIgnore
    private transient final Pattern flinkVersionPattern = Pattern.compile("^Version: (.*), Commit ID: (.*)$");

    @JsonIgnore
    public String extractFlinkVersion(String path) {
        assert path != null;
        AtomicReference<String> flinkVersion = new AtomicReference<>();
        if (path != null) {
            String libPath = path.concat("/lib");
            File[] distJar = new File(libPath).listFiles(x -> x.getName().matches("flink-dist_.*\\.jar"));
            if (distJar == null || distJar.length == 0) {
                throw new IllegalArgumentException("[StreamX] can no found flink-dist jar in " + libPath);
            }
            if (distJar.length > 1) {
                throw new IllegalArgumentException("[StreamX] found multiple flink-dist jar in " + libPath);
            }
            List<String> cmd = Arrays.asList(
                "cd ".concat(path),
                String.format(
                    "java -classpath %s org.apache.flink.client.cli.CliFrontend --version",
                    distJar[0].getAbsolutePath()
                )
            );

            CommandUtils.execute(cmd, versionInfo -> {
                Matcher matcher = flinkVersionPattern.matcher(versionInfo);
                if (matcher.find()) {
                    flinkVersion.set(matcher.group(1));
                }
            });
        }
        return flinkVersion.get();
    }

    public void doSetFlinkConf() throws IOException {
        assert this.flinkHome != null;
        File yaml = new File(this.flinkHome.concat("/conf/flink-conf.yaml"));
        assert yaml.exists();
        String flinkConf = FileUtils.readFileToString(yaml);
        this.flinkConf = DeflaterUtils.zipString(flinkConf);
    }

    public void doSetVersion() throws IOException {
        assert this.flinkHome != null;
        this.setVersion(this.extractFlinkVersion(this.getFlinkHome()));
    }

    @JsonIgnore
    public Map<String, String> convertFlinkYamlAsMap() {
        String flinkYamlString = DeflaterUtils.unzipString(flinkConf);
        return PropertiesUtils.loadFlinkConfYaml(flinkYamlString);
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
