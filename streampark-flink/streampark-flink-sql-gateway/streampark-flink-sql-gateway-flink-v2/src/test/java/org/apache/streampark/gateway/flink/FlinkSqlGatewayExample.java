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

package org.apache.streampark.gateway.flink;

import org.apache.streampark.gateway.flink.v2.client.dto.ExecuteStatementRequestBody;
import org.apache.streampark.gateway.flink.v2.client.dto.ExecuteStatementResponseBody;
import org.apache.streampark.gateway.flink.v2.client.dto.FetchResultsResponseBody;
import org.apache.streampark.gateway.flink.v2.client.dto.OpenSessionRequestBody;
import org.apache.streampark.gateway.flink.v2.client.dto.OpenSessionResponseBody;
import org.apache.streampark.gateway.flink.v2.client.dto.RowFormat;
import org.apache.streampark.gateway.flink.v2.client.rest.ApiException;
import org.apache.streampark.gateway.flink.v2.client.rest.DefaultApi;

public class FlinkSqlGatewayExample {

  private FlinkSqlGatewayExample() {}

  public static void main(String[] args) throws Exception {
    DefaultApi api = FlinkSqlGateway.sqlGatewayApi("http://192.168.20.239:8083");
    runOnRemote(api);
    //    runOnYarn(api);
    //    runOnKubernetes(api);
  }

  public static void runOnRemote(DefaultApi api) throws ApiException, InterruptedException {
    OpenSessionResponseBody response =
        api.openSession(
            new OpenSessionRequestBody()
                .putPropertiesItem("rest.address", "192.168.20.239")
                .putPropertiesItem("rest.port", "8081")
                .putPropertiesItem("execution.target", "remote"));
    String sessionHandle = response.getSessionHandle();
    System.out.println("SessionHandle: " + sessionHandle);

    ExecuteStatementResponseBody statement1 =
        api.executeStatement(
            sessionHandle,
            new ExecuteStatementRequestBody()
                .statement(
                    "CREATE TABLE Orders (\n"
                        + "    order_number BIGINT,\n"
                        + "    price        DECIMAL(32,2),\n"
                        + "    buyer        ROW<first_name STRING, last_name STRING>,\n"
                        + "    order_time   TIMESTAMP(3)\n"
                        + ") WITH (\n"
                        + "  'connector' = 'datagen',\n"
                        + "  'number-of-rows' = '2'\n"
                        + ")")
                .putExecutionConfigItem(
                    "pipeline.name", "Flink SQL Gateway SDK on flink cluster Example"));

    System.out.println("create table: " + statement1.getOperationHandle());

    ExecuteStatementResponseBody statement2 =
        api.executeStatement(
            sessionHandle,
            new ExecuteStatementRequestBody()
                .statement("select * from Orders;")
                .putExecutionConfigItem(
                    "pipeline.name", "Flink SQL Gateway SDK on flink cluster Example"));

    System.out.println("select * from Orders: " + statement2.getOperationHandle());

    Thread.sleep(1000 * 10);

    FetchResultsResponseBody fetchResultsResponseBody =
        api.fetchResults(sessionHandle, statement2.getOperationHandle(), 0L, RowFormat.JSON);
    System.out.println(fetchResultsResponseBody.getResults());
  }

  //  public static void main(String[] args) {
  //    System.out.println(
  //        "CREATE TABLE Orders (\n"
  //            + "    order_number BIGINT,\n"
  //            + "    price        DECIMAL(32,2),\n"
  //            + "    buyer        ROW<first_name STRING, last_name STRING>,\n"
  //            + "    order_time   TIMESTAMP(3)\n"
  //            + ") WITH (\n"
  //            + "  'connector' = 'datagen',\n"
  //            + "  'number-of-rows' = '2'\n"
  //            + ")");
  //  }
}
