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
import org.apache.streampark.common.enums.ClusterStateEnum;
import org.apache.streampark.common.enums.ExecutionModeEnum;
import org.apache.streampark.common.enums.FlinkK8sRestExposedTypeEnum;
import org.apache.streampark.common.enums.ResolveOrderEnum;
import org.apache.streampark.common.util.HttpClientUtils;
import org.apache.streampark.common.util.PropertiesUtils;
import org.apache.streampark.console.base.util.JacksonUtils;
import org.apache.streampark.console.core.utils.YarnQueueLabelExpression;

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.configuration.CoreOptions;
import org.apache.hc.client5.http.config.RequestConfig;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Data;
import lombok.SneakyThrows;

import java.io.Serializable;
import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Data
@TableName("t_flink_cluster")
public class FlinkCluster implements Serializable {

  @TableId(type = IdType.AUTO)
  private Long id;

  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private String address;

  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private String jobManagerUrl;

  private String clusterId;

  private String clusterName;

  private Integer executionMode;

  /** flink version */
  private Long versionId;

  private String k8sNamespace;

  private String serviceAccount;

  private String description;

  private Long userId;

  private String flinkImage;

  private String options;

  private String yarnQueue;

  private Boolean k8sHadoopIntegration;

  private String dynamicProperties;

  private Integer k8sRestExposedType;

  private String k8sConf;

  private Integer resolveOrder;

  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private String exception;

  private Integer clusterState;

  private Date createTime = new Date();

  private Date startTime;

  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private Date endTime;

  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private Long alertId;

  private transient Integer allJobs = 0;

  private transient Integer affectedJobs = 0;

  @JsonIgnore
  public FlinkK8sRestExposedTypeEnum getK8sRestExposedTypeEnum() {
    return FlinkK8sRestExposedTypeEnum.of(this.k8sRestExposedType);
  }

  public ExecutionModeEnum getExecutionModeEnum() {
    return ExecutionModeEnum.of(this.executionMode);
  }

  public ClusterStateEnum getClusterStateEnum() {
    return ClusterStateEnum.of(this.clusterState);
  }

  @JsonIgnore
  @SneakyThrows
  public Map<String, Object> getOptionMap() {
    if (StringUtils.isBlank(this.options)) {
      return Collections.emptyMap();
    }
    Map<String, Object> map = JacksonUtils.read(this.options, Map.class);
    if (ExecutionModeEnum.YARN_SESSION == getExecutionModeEnum()) {
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
          this.address,
          RequestConfig.custom().setConnectTimeout(2000, TimeUnit.MILLISECONDS).build());
      return new URI(address);
    } catch (Exception ignored) {
      //
    }
    return null;
  }

  @JsonIgnore
  public Map<String, String> getFlinkConfig() throws JsonProcessingException {
    String restUrl = this.address + "/jobmanager/config";
    String json =
        HttpClientUtils.httpGetRequest(
            restUrl, RequestConfig.custom().setConnectTimeout(2000, TimeUnit.MILLISECONDS).build());
    if (StringUtils.isBlank(json)) {
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
    ResolveOrderEnum resolveOrderEnum = ResolveOrderEnum.of(this.getResolveOrder());
    if (resolveOrderEnum != null) {
      map.put(CoreOptions.CLASSLOADER_RESOLVE_ORDER.key(), resolveOrderEnum.getName());
    }
    return map;
  }

  public static class SFunc {
    public static final SFunction<FlinkCluster, Long> ID = FlinkCluster::getId;
    public static final SFunction<FlinkCluster, String> ADDRESS = FlinkCluster::getAddress;
    public static final SFunction<FlinkCluster, String> JOB_MANAGER_URL =
        FlinkCluster::getJobManagerUrl;
    public static final SFunction<FlinkCluster, Integer> CLUSTER_STATE =
        FlinkCluster::getClusterState;
    public static final SFunction<FlinkCluster, Integer> EXECUTION_MODE =
        FlinkCluster::getExecutionMode;
    public static final SFunction<FlinkCluster, String> EXCEPTION = FlinkCluster::getException;
  }
}
