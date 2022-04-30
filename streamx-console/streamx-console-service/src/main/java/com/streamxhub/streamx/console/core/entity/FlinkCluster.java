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

import com.streamxhub.streamx.common.conf.ConfigConst;
import com.streamxhub.streamx.common.enums.ClusterState;
import com.streamxhub.streamx.common.enums.ExecutionMode;
import com.streamxhub.streamx.common.enums.FlinkK8sRestExposedType;
import com.streamxhub.streamx.common.util.HttpClientUtils;
import com.streamxhub.streamx.console.base.util.JacksonUtils;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.http.client.config.RequestConfig;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author benjobs
 */
@Data
@TableName("t_flink_cluster")
public class FlinkCluster implements Serializable {

    private Long id;

    private String address;

    private String clusterId;

    private String clusterName;

    private Integer executionMode;

    /**
     * 对应的flink的版本.
     */
    private Long versionId;

    private String k8sNamespace;

    private String serviceAccount;

    private String description;

    private Long userId;

    private String flinkImage;

    private String options;

    private String yarnQueue;

    private Boolean k8sHadoopIntegration;

    private String dynamicOptions;

    private Integer k8sRestExposedType;

    private Boolean flameGraph;

    private String k8sConf;

    private Integer resolveOrder;

    private String exception;

    private Integer clusterState;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime = new Date();

    @JsonIgnore
    public FlinkK8sRestExposedType getK8sRestExposedTypeEnum() {
        return FlinkK8sRestExposedType.of(this.k8sRestExposedType);
    }

    public ExecutionMode getExecutionModeEnum() {
        return ExecutionMode.of(this.executionMode);
    }

    public ClusterState getClusterStateEnum() {
        return ClusterState.of(this.clusterState);
    }

    @JsonIgnore
    @SneakyThrows
    public Map<String, Object> getOptionMap() {
        Map<String, Object> map = JacksonUtils.read(getOptions(), Map.class);
        if (ExecutionMode.YARN_SESSION.equals(getExecutionModeEnum())) {
            map.put(ConfigConst.KEY_YARN_APP_NAME(), this.clusterName);
            map.put(ConfigConst.KEY_YARN_APP_QUEUE(), this.yarnQueue);
        }
        map.entrySet().removeIf(entry -> entry.getValue() == null);
        return map;
    }

    @JsonIgnore
    public URI getActiveAddress() {
        String[] array = address.split(",");
        for (String url : array) {
            try {
                HttpClientUtils.httpGetRequest(url, RequestConfig.custom().setSocketTimeout(2000).build());
                return new URI(url);
            } catch (Exception ignored) {
                //
            }
        }
        return null;
    }

    @JsonIgnore
    public boolean verifyConnection() {
        if (address == null) {
            return false;
        }
        String[] array = address.split(",");
        for (String url : array) {
            try {
                // 检查是否有效
                new URI(url);
            } catch (Exception ignored) {
                return false;
            }
            try {
                HttpClientUtils.httpGetRequest(url, RequestConfig.custom().setConnectTimeout(2000).build());
                return true;
            } catch (Exception ignored) {
                //
            }
        }
        return false;
    }

    @JsonIgnore
    public Map<String, String> getFlinkConfig() throws MalformedURLException, JsonProcessingException {
        URI activeAddress = this.getActiveAddress();
        String restUrl = activeAddress.toURL() + "/jobmanager/config";
        String json = HttpClientUtils.httpGetRequest(restUrl, RequestConfig.custom().setConnectTimeout(2000).build());
        List<Map<String, String>> confList = JacksonUtils.read(json, new TypeReference<List<Map<String, String>>>() {
        });
        Map<String, String> config = new HashMap<>(0);
        confList.forEach(k -> config.put(k.get("key"), k.get("value")));
        return config;
    }

}
