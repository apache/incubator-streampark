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

import org.apache.streampark.console.core.enums.EngineTypeEnum;
import org.apache.streampark.console.core.enums.ResourceTypeEnum;

import org.apache.commons.lang3.StringUtils;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("t_resource")
public class Resource implements Serializable {

  private static final long serialVersionUID = 1L;

  @TableId(type = IdType.AUTO)
  private Long id;

  // resourceName unique
  private String resourceName;

  // resource path
  private String resourcePath;

  private String resource;

  @Size(max = 100, message = "{noMoreThan}")
  private String description;

  /** user id of creator */
  private Long creatorId;

  private ResourceTypeEnum resourceType;

  private EngineTypeEnum engineType;

  // for flink app
  private String mainClass;

  // for flink connector
  private String connectorRequiredOptions;

  // for flink connector
  private String connectorOptionalOptions;

  /** user name of creator */
  private transient String creatorName;

  @NotNull(message = "{required}")
  private Long teamId;

  private Date createTime;

  private Date modifyTime;

  private transient String connector;

  public void setResourcePath(String resourcePath) {
    if (StringUtils.isBlank(resourcePath)) {
      throw new IllegalArgumentException("resource path cannot be null.");
    }
    String[] namePath = resourcePath.split(":");
    if (namePath.length != 2) {
      throw new IllegalArgumentException("resource path invalid, format: $name:$path");
    }
    this.resourcePath = resourcePath;
  }

  public String getFileName() {
    if (StringUtils.isBlank(this.resourcePath)) {
      return null;
    }
    return resourcePath.split(":")[0];
  }

  public String getFilePath() {
    if (StringUtils.isBlank(this.resourcePath)) {
      return null;
    }
    return resourcePath.split(":")[1];
  }
}
