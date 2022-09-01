/*
 * Copyright 2019 The StreamX Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.console.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

/**
 * @author benjobs
 */
@Data
@TableName("t_setting")
@Slf4j
public class Setting implements Serializable {

    private Integer orderNum;

    private String settingName;

    @TableId(type = IdType.INPUT)
    private String settingKey;

    private String settingValue;

    private Integer type;

    private String description;
    private transient boolean editable = false;
    private transient boolean submitting = false;

    private transient String flinkHome;
    private transient String flinkConf;

}
