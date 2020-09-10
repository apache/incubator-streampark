/**
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.monitor.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.streamxhub.common.conf.ConfigConst;
import com.wuwenze.poi.annotation.Excel;
import lombok.Data;
/**
 * @author benjobs
 */
@Data
@TableName("t_app_backup")
@Excel("app备份实体")
public class ApplicationBackUp {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private Long appId;
    private String path;
    private String description;
    private Long timeStamp;

    public ApplicationBackUp(Application application) {
        this.appId = application.getId();
        this.description = application.getBackUpDescription();
        this.timeStamp = System.currentTimeMillis();
        this.path = ConfigConst.APP_HISTORY()
                .concat("/")
                .concat(application.getId().toString())
                .concat("/")
                .concat(timeStamp.toString());
    }

}
