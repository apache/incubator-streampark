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
package com.streamxhub.console.core.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.wuwenze.poi.annotation.Excel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * @author benjobs
 */
@Data
@TableName("t_flame_graph")
@Excel("火焰图")
@Slf4j
public class FlameGraph {

    private Long id;

    private Long appId;

    private String profiler;

    private Date timeline;

    private String content;

    private transient Date start = new Date();

    private transient Integer duration = 60 * 4;

    private final transient Integer DAY_OF_MINUTE_DURATION = 60 * 24;


    @JsonIgnore
    public Date getEnd() {
        if (this.duration > DAY_OF_MINUTE_DURATION) {
            throw new IllegalArgumentException("[StreamX] flameGraph duration cannot be greater than 24 hours");
        }
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getDefault());
        cal.setTime(this.getStart());
        cal.add(Calendar.MINUTE, duration);
        return cal.getTime();
    }

    public String getFlameGraphJsonName() {
        return String.format("%d_%d_%d.json", this.appId, start.getTime(), getEnd().getTime());
    }

}
