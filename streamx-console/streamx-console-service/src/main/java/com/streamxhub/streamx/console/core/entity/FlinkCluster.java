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

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.streamxhub.streamx.common.util.HttpClientUtils;
import lombok.Data;
import org.apache.http.client.config.RequestConfig;

import java.io.Serializable;
import java.net.URI;
import java.util.Date;

/**
 * @author benjobs
 */
@Data
@TableName("t_flink_cluster")
public class FlinkCluster implements Serializable {

    private Long id;

    private String clusterName;

    private String address;

    private String description;

    private Date createTime = new Date();

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
}
