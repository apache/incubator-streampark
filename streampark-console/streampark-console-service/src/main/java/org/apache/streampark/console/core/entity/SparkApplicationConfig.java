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

import org.apache.streampark.common.util.DeflaterUtils;
import org.apache.streampark.common.util.PropertiesUtils;
import org.apache.streampark.console.core.enums.ConfigFileTypeEnum;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
@Getter
@Setter
@TableName("t_spark_config")
@Slf4j
public class SparkApplicationConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long appId;

    /**
     * 1)yaml <br>
     * 2)prop <br>
     * 3)hocon
     */
    private Integer format;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String content;

    /** default version: 1 */
    private Integer version = 1;

    /** record the configuration to take effect for the target */
    private Boolean latest;

    private Date createTime;

    private transient boolean effective = false;

    public void setToApplication(SparkApplication application) {
        String unzipString = DeflaterUtils.unzipString(content);
        String encode = Base64.getEncoder().encodeToString(unzipString.getBytes());
        application.setConfig(encode);
        application.setConfigId(this.id);
        application.setFormat(this.format);
    }

    @Nullable
    private Map<String, String> readConfig() {
        ConfigFileTypeEnum fileType = ConfigFileTypeEnum.of(this.format);
        if (fileType == null) {
            return null;
        }
        switch (fileType) {
            case YAML:
                return PropertiesUtils.fromYamlTextAsJava(DeflaterUtils.unzipString(this.content));
            case PROPERTIES:
                return PropertiesUtils.fromPropertiesTextAsJava(DeflaterUtils.unzipString(this.content));
            case HOCON:
                return PropertiesUtils.fromHoconTextAsJava(DeflaterUtils.unzipString(this.content));
            default:
                return new HashMap<>();
        }
    }
}
