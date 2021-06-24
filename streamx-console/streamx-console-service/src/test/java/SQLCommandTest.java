/*
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

import com.streamxhub.streamx.common.enums.SqlErrorType;
import com.streamxhub.streamx.console.base.domain.RestResponse;
import com.streamxhub.streamx.flink.core.SqlError;
import com.streamxhub.streamx.flink.core.FlinkSqlValidator;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SQLCommandTest {

    @Test
    public void sqlParae() {

        String sql = "SET table.planner = blink;";

        SqlError sqlError = FlinkSqlValidator.verifySql(sql);
        if (sqlError != null) {
            String[] array = sqlError.sql().trim().split("\n");
            String start = array[0].trim();
            String end = array.length > 1 ? array[array.length - 1].trim() : null;

            //记录错误类型,出错的sql,原因,错误的开始行和结束行内容(用于前端查找mod节点)
            RestResponse response = RestResponse.create()
                    .data(false)
                    .message(sqlError.exception())
                    .put("type", sqlError.errorType().errorType)
                    .put("sql", sqlError.sql())
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

            System.out.println(response);
        }

    }

    @Test
    public void regexp() {
        String regex = "SET(\\s+(\\S+)\\s*=(.*))?";
        String input = "SET table.planner = blink";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(input);
        boolean xx = matcher.matches();
        System.out.println(xx);
    }

}
