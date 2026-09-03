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

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/** Persistent metadata for a locally installed Flink distribution. */
@Getter
@Setter
@TableName("t_flink_env")
public class FlinkEnv implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String flinkName;

    private String flinkHome;

    private String flinkConf;

    private String description;

    private String scalaVersion;

    private String version;

    /** Whether this installation is the default Flink environment. */
    private Boolean isDefault;

    private Date createTime;
}
