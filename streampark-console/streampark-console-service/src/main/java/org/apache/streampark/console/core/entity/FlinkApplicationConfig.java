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

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/** Persistent version of an application's user-supplied Flink configuration. */
@Getter
@Setter
@TableName("t_flink_config")
public class FlinkApplicationConfig {

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
}
