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

import java.util.Date;

/**
 * @author benjobs
 */
@Data
@TableName("t_flink_log")
@Slf4j
public class ApplicationLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * appId
     */
    private Long appId;
    /**
     * applicationId
     */
    private String yarnAppId;
    /**
     * 启动状态
     */
    private Boolean success;

    /**
     * 操作时间
     */
    private Date optionTime;

    /**
     * 启动失败的异常
     */
    private String exception;
}
