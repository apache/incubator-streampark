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

import org.apache.streampark.common.conf.ConfigConst;
import org.apache.streampark.common.enums.ClusterState;
import org.apache.streampark.common.enums.ExecutionMode;
import org.apache.streampark.common.enums.FlinkK8sRestExposedType;
import org.apache.streampark.common.enums.ResolveOrder;
import org.apache.streampark.common.util.HttpClientUtils;
import org.apache.streampark.common.util.PropertiesUtils;
import org.apache.streampark.common.util.YarnUtils;
import org.apache.streampark.console.base.util.CommonUtils;
import org.apache.streampark.console.base.util.JacksonUtils;
import org.apache.streampark.console.core.metrics.flink.Overview;
import org.apache.streampark.console.core.utils.YarnQueueLabelExpression;

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.configuration.CoreOptions;
import org.apache.http.client.config.RequestConfig;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Getter
@Setter
@TableName("t_flink_cluster")
public class FlinkCluster implements Serializable {

  @TableId(type = IdType.AUTO)
  private Long id;

  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private String address;

  private String clusterId;

  private String clusterName;

  private Integer executionMode;

  /** flink version */
  private Long versionId;

  private String k8sNamespace;

  private String serviceAccount;

  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private String description;

  private Long userId;

  private String flinkImage;

  private String options;

  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private String yarnQueue;

  private Boolean k8sHadoopIntegration;

  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private String dynamicProperties;

  private Integer k8sRestExposedType;

  private String k8sConf;

  private Integer resolveOrder;

  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private String exception;

  private Integer clusterState;

  private Date createTime;

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
    if (StringUtils.isBlank(this.options)) {
      return Collections.emptyMap();
    }
    Map<String, Object> map = JacksonUtils.read(this.options, Map.class);
    if (ExecutionMode.YARN_SESSION.equals(getExecutionModeEnum())) {
      map.put(ConfigConst.KEY_YARN_APP_NAME(), this.clusterName);
      map.putAll(YarnQueueLabelExpression.getQueueLabelMap(yarnQueue));
    }
    map.entrySet().removeIf(entry -> entry.getValue() == null);
    return map;
  }

  @JsonIgnore
  public URI getRemoteURI() {
    try {
      HttpClientUtils.httpGetRequest(
          this.address, RequestConfig.custom().setSocketTimeout(2000).build());
      return new URI(address);
    } catch (Exception e) {
      log.error("Get remote URI failed!", e);
    }
    return null;
  }

  public boolean verifyClusterConnection() {
    if (ExecutionMode.REMOTE.equals(this.getExecutionModeEnum())) {
      if (address == null) {
        return false;
      }
      // 1) check url is Legal
      if (!CommonUtils.isLegalUrl(address)) {
        return false;
      }
      // 2) check connection
      try {
        String restUrl = address + "/overview";
        String result =
            HttpClientUtils.httpGetRequest(
                restUrl, RequestConfig.custom().setConnectTimeout(2000).build());
        JacksonUtils.read(result, Overview.class);
        return true;
      } catch (Exception e) {
        log.error("Verify cluster {} connection failed!", address, e);
      }
      return false;
    } else if (ExecutionMode.YARN_SESSION.equals(this.getExecutionModeEnum())) {
      try {
        String restUrl = YarnUtils.getRMWebAppURL(true) + "/proxy/" + this.clusterId + "/overview";
        String result = YarnUtils.restRequest(restUrl, 2000);
        JacksonUtils.read(result, Overview.class);
        return true;
      } catch (Exception e) {
        log.error("Verify cluster connection failed!", e);
      }
      return false;
    }
    return false;
  }

  @JsonIgnore
  public Map<String, String> getFlinkConfig() throws JsonProcessingException {
    String restUrl = this.address + "/jobmanager/config";
    String json =
        HttpClientUtils.httpGetRequest(
            restUrl, RequestConfig.custom().setConnectTimeout(2000).build());
    if (StringUtils.isEmpty(json)) {
      return Collections.emptyMap();
    }
    List<Map<String, String>> confList =
        JacksonUtils.read(json, new TypeReference<List<Map<String, String>>>() {});
    Map<String, String> config = new HashMap<>(0);
    confList.forEach(k -> config.put(k.get("key"), k.get("value")));
    return config;
  }

  @JsonIgnore
  public Map<String, Object> getProperties() {
    Map<String, Object> map = new HashMap<>();
    Map<String, String> dynamicProperties =
        PropertiesUtils.extractDynamicPropertiesAsJava(this.getDynamicProperties());
    map.putAll(this.getOptionMap());
    map.putAll(dynamicProperties);
    ResolveOrder resolveOrder = ResolveOrder.of(this.getResolveOrder());
    if (resolveOrder != null) {
      map.put(CoreOptions.CLASSLOADER_RESOLVE_ORDER.key(), resolveOrder.getName());
    }
    return map;
  }
}
