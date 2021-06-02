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
import com.streamxhub.streamx.console.base.domain.RestResponse;
import com.streamxhub.streamx.flink.core.SqlValidator;
import org.junit.Test;

public class SQLCommandTest {

    @Test
    public void sqlParae() {

        String sql = "CREATE VIEW ods_oms_unify_transport_order_relation_view AS " +
                "SELECT ur.*, u.* " +
                "FROM ods_oms_unify_transport_order_relation ur " +
                "join ods_oms_unify_order u " +
                "ON ur.unify_order_no = u.unify_odr_no;";

        RestResponse response = null;

        try {
            SqlValidator.verifySQL(sql);
            response = RestResponse.create().data(true);
        } catch (Exception e) {
            String split = ",sql:";
            String message = e.getMessage();

            String errorMsg = message.substring(0, message.indexOf(split)).trim();
            String errorSQL = message.substring(message.indexOf(split) + split.length()).trim();

            String[] array = errorSQL.split("\n");
            String first = array[0];
            String end = array.length > 1 ? array[array.length - 1] : null;

            response = RestResponse.create()
                    .data(false)
                    .message(errorMsg)
                    .put("sql", errorSQL)
                    .put("first", first)
                    .put("end", end);

        }

        System.out.println(response);
    }

}
