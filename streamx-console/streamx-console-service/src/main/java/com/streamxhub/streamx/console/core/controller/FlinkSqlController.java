/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.console.core.controller;

import com.streamxhub.streamx.common.enums.SqlErrorType;
import com.streamxhub.streamx.console.base.domain.RestResponse;
import com.streamxhub.streamx.console.base.exception.ServiceException;
import com.streamxhub.streamx.console.core.entity.Application;
import com.streamxhub.streamx.console.core.entity.FlinkSql;
import com.streamxhub.streamx.console.core.service.FlinkSqlService;
import com.streamxhub.streamx.console.core.service.SqlCompleteService;
import com.streamxhub.streamx.flink.core.SqlError;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;

import java.util.List;

/**
 * @author benjobs
 */
@Api(tags = "[flink sql]相关操作", consumes = "Content-Type=application/x-www-form-urlencoded")
@Slf4j
@Validated
@RestController
@RequestMapping("flink/sql")
public class FlinkSqlController {

    @Autowired
    private FlinkSqlService flinkSqlService;

    @Autowired
    private SqlCompleteService sqlComplete;

    @PostMapping("verify")
    public RestResponse verify(String sql, Long versionId) {
        SqlError sqlError = flinkSqlService.verifySql(sql, versionId);
        if (sqlError != null) {
            String[] array = sqlError.sql().trim().split("\n");
            String start = array[0].trim();
            String end = array.length > 1 ? array[array.length - 1].trim() : null;

            //记录错误类型,出错的sql,原因,错误的开始行和结束行内容(用于前端查找mod节点)
            RestResponse response = RestResponse.create()
                .data(false)
                .message(sqlError.exception())
                .put("type", sqlError.errorType().getValue())
                .put("start", start)
                .put("end", end);
            //语法异常
            if (sqlError.errorType().equals(SqlErrorType.SYNTAX_ERROR)) {
                String exception = sqlError.exception().replaceAll("[\r\n]", "");
                String sqlParseFailedRegexp = "SQL\\sparse\\sfailed\\.\\sEncountered\\s\"(.*)\"\\sat\\sline\\s\\d,\\scolumn\\s\\d.*";
                if (exception.matches(sqlParseFailedRegexp)) {
                    String[] lineColumn = exception
                        .replaceAll("^.*\\sat\\sline\\s", "")
                        .replaceAll(",\\scolumn\\s", ",")
                        .replaceAll("\\.(.*)", "")
                        .trim()
                        .split(",");

                    //记录第几行出错.
                    response.put("line", lineColumn[0])
                        .put("column ", lineColumn[1]);
                }
            }
            return response;
        } else {
            return RestResponse.create().data(true);
        }
    }

    @PostMapping("get")
    public RestResponse get(String id) throws ServiceException {
        String[] array = id.split(",");
        FlinkSql flinkSql1 = flinkSqlService.getById(array[0]);
        flinkSql1.base64Encode();
        if (array.length == 1) {
            return RestResponse.create().data(flinkSql1);
        }
        FlinkSql flinkSql2 = flinkSqlService.getById(array[1]);
        flinkSql2.base64Encode();
        return RestResponse.create().data(new FlinkSql[]{flinkSql1, flinkSql2});
    }

    @PostMapping("history")
    public RestResponse sqlhistory(Application application) {
        List<FlinkSql> sqlList = flinkSqlService.history(application);
        return RestResponse.create().data(sqlList);
    }

    @PostMapping("sqlComplete")
    public RestResponse getSqlComplete(@NotNull(message = "{required}") String sql) {
        return RestResponse.create().put("word", sqlComplete.getComplete(sql));
    }

}
