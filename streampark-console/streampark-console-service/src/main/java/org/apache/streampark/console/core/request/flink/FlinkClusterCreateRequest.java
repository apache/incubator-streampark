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

package org.apache.streampark.console.core.request.flink;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * Request body for {@code POST /flink/cluster/create}, aligned with webapp {@code FlinkCluster}.
 */
@Getter
@Setter
public class FlinkClusterCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String address;

    private String jobManagerUrl;

    private String clusterId;

    @NotBlank
    private String clusterName;

    @NotNull
    private Integer deployMode;

    private Long versionId;

    private String k8sNamespace;

    private String serviceAccount;

    private String description;

    private String flinkImage;

    private String options;

    private String yarnQueue;

    private Boolean k8sHadoopIntegration;

    private String dynamicProperties;

    private Integer k8sRestExposedType;

    private String k8sConf;

    private Integer resolveOrder;

    private Long alertId;
}
