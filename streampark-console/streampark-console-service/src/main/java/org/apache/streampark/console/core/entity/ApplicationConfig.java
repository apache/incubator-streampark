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
import org.apache.streampark.common.util.DeflaterUtils;
import org.apache.streampark.common.util.PropertiesUtils;
import org.apache.streampark.console.core.enums.ConfigFileType;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@TableName("t_flink_config")
@Slf4j
public class ApplicationConfig {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long appId;

  /**
   * 1)yaml <br>
   * 2)prop <br>
   * 3)hocon
   */
  private Integer format;

  /** default version: 1 */
  private Integer version = 1;

  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private String content;

  private Date createTime;

  /** record the configuration to take effect for the target */
  private Boolean latest;

  private transient boolean effective = false;

  public void setToApplication(Application application) {
    String unzipString = DeflaterUtils.unzipString(content);
    String encode = Base64.getEncoder().encodeToString(unzipString.getBytes());
    application.setConfig(encode);
    application.setConfigId(this.id);
    application.setFormat(this.format);
  }

  public Map<String, String> readConfig() {
    ConfigFileType fileType = ConfigFileType.of(this.format);
    Map<String, String> configs = null;
    if (fileType != null) {
      switch (fileType) {
        case YAML:
          configs = PropertiesUtils.fromYamlTextAsJava(DeflaterUtils.unzipString(this.content));
          break;
        case PROPERTIES:
          configs =
              PropertiesUtils.fromPropertiesTextAsJava(DeflaterUtils.unzipString(this.content));
          break;
        case HOCON:
          configs = PropertiesUtils.fromHoconTextAsJava(DeflaterUtils.unzipString(this.content));
          break;
        default:
          configs = Collections.emptyMap();
          break;
      }
    }

    if (configs != null && !configs.isEmpty()) {
      return configs.entrySet().stream()
          .collect(
              Collectors.toMap(
                  entry -> {
                    String key = entry.getKey();
                    if (key.startsWith(ConfigConst.KEY_FLINK_OPTION_PREFIX())) {
                      key = key.substring(ConfigConst.KEY_FLINK_OPTION_PREFIX().length());
                    } else if (key.startsWith(ConfigConst.KEY_FLINK_PROPERTY_PREFIX())) {
                      key = key.substring(ConfigConst.KEY_FLINK_PROPERTY_PREFIX().length());
                    } else if (key.startsWith(ConfigConst.KEY_FLINK_TABLE_PREFIX())) {
                      key = key.substring(ConfigConst.KEY_FLINK_TABLE_PREFIX().length());
                    } else if (key.startsWith(ConfigConst.KEY_APP_PREFIX())) {
                      key = key.substring(ConfigConst.KEY_APP_PREFIX().length());
                    } else if (key.startsWith(ConfigConst.KEY_SQL_PREFIX())) {
                      key = key.substring(ConfigConst.KEY_SQL_PREFIX().length());
                    }
                    return key;
                  },
                  Map.Entry::getValue));
    }
    return Collections.emptyMap();
  }
}
