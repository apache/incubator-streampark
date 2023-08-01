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

import org.apache.streampark.gateway.flink.client.dto.ExecuteStatementRequestBody;
import org.apache.streampark.gateway.flink.client.dto.ExecuteStatementResponseBody;
import org.apache.streampark.gateway.flink.client.dto.FetchResultsResponseBody;
import org.apache.streampark.gateway.flink.client.dto.OpenSessionRequestBody;
import org.apache.streampark.gateway.flink.client.dto.OpenSessionResponseBody;
import org.apache.streampark.gateway.flink.client.dto.ResultSetDataInner;
import org.apache.streampark.gateway.flink.client.rest.ApiException;
import org.apache.streampark.gateway.flink.client.rest.v1.DefaultApi;

import java.util.UUID;
import java.util.stream.Collectors;

public class FlinkSqlGatewayExample {

  private FlinkSqlGatewayExample() {}

  public static void main(String[] args) throws Exception {
    DefaultApi api = FlinkSqlGateway.sqlGatewayApi("http://192.168.20.144:8083");
    runOnRemote(api);
    //    runOnYarn(api);
    //    runOnKubernetes(api);
  }

  public static void runOnRemote(DefaultApi api) throws Exception {
    OpenSessionResponseBody response =
        api.openSession(
            new OpenSessionRequestBody()
                .putPropertiesItem("rest.address", "192.168.20.239")
                //                    .putPropertiesItem("rest.address", "192.168.20.144")
                .putPropertiesItem("rest.port", "8081")
                .putPropertiesItem("execution.target", "remote"));
    String sessionHandle = response.getSessionHandle();
    printResult(api, sessionHandle, extracted(api, sessionHandle, "show catalogs;"));

    printResult(
        api,
        sessionHandle,
        extracted(
            api,
            sessionHandle,
            "CREATE CATALOG my_catalog WITH(\n"
                + "    'type' = 'jdbc',\n"
                + "    'default-database' = 'streampark',\n"
                + "    'username' = 'root',\n"
                + "    'password' = 'streampark',\n"
                + "    'base-url' = 'jdbc:mysql://192.168.20.144:3306'\n"
                + ");\n"));

    //    printResult(
    //        api,
    //        sessionHandle,
    //        extracted(
    //            api,
    //            sessionHandle,
    //            "CREATE CATALOG mysql_sp WITH (\n"
    //                + "  'type' = 'streampark_mysql_catalog',\n"
    //                + "  'jdbcUrl' =
    // 'jdbc:mysql://localhost:3306/streampark?useSSL=false&useUnicode=true&characterEncoding=UTF-8',\n"
    //                + "  'username' = 'root',\n"
    //                + "  'password' = 'streampark'\n"
    //                + ");"));
    //    printResult(api, sessionHandle, extracted(api, sessionHandle, "USE CATALOG mysql_sp;"));
    //        printResult(api, sessionHandle, extracted(api, sessionHandle, "CREATE TABLE source3 ("
    //            + "id STRING"
    //            + ") WITH (" +
    //            " 'connector' = 'datagen'" +
    //            ")"));
    printResult(api, sessionHandle, extracted(api, sessionHandle, "USE CATALOG my_catalog"));
    printResult(api, sessionHandle, extracted(api, sessionHandle, "show tables;"));

    //    printResult(
    //        api,
    //        sessionHandle,
    //        extracted(
    //            api,
    //            sessionHandle,
    //            "ADD JAR '/opt/flink-1.16.1/udf/streampark-flink-udf_2.12-2.2.0-SNAPSHOT.jar';"));
    //        printResult(api, sessionHandle, extracted(api, sessionHandle, "create function  if not
    // exists length_function as 'org.apache.streampark.flink.udf.Length' language scala"));
    //        printResult(api, sessionHandle, extracted(api, sessionHandle, " DESCRIBE mykafka;"));
    //        printResult(api, sessionHandle, extracted(api, sessionHandle, " show tables;"));
    //        printResult(api, sessionHandle, extracted(api, sessionHandle, " show databases;"));
    //    printResult(
    //        api,
    //        sessionHandle,
    //        extracted(api, sessionHandle, " select length_function(id) from source3;"));

    printResult(
        api, sessionHandle, extracted(api, sessionHandle, " show create table t_flink_app;"));
  }

  private static void printResult(DefaultApi api, String sessionHandle, String statementHandle)
      throws Exception {

    if (statementHandle != null) {
      try {
        System.out.println("---start---");
        System.out.println(
            "operationStatus: "
                + api.getOperationStatus(
                        UUID.fromString(sessionHandle), UUID.fromString(statementHandle))
                    .getStatus());
        Thread.sleep(5 * 1000);
        FetchResultsResponseBody fetchResultsResponseBody =
            api.fetchResults(UUID.fromString(sessionHandle), UUID.fromString(statementHandle), 0L);
        System.out.println(
            fetchResultsResponseBody.getResults().getColumns().stream()
                .map(o -> o.getName() + " " + o.getLogicalType().getType())
                .collect(Collectors.toList()));
        for (ResultSetDataInner datum : fetchResultsResponseBody.getResults().getData()) {
          datum.getFields().forEach(o -> System.out.println(o + " "));
        }
        System.out.println("---end---");
      } catch (ApiException e) {
        System.out.println(e.getResponseBody());
      }
    }
  }

  private static String extracted(DefaultApi api, String sessionHandle, String sql)
      throws ApiException {
    ExecuteStatementResponseBody statement1 = null;
    try {
      statement1 =
          api.executeStatement(
              UUID.fromString(sessionHandle),
              new ExecuteStatementRequestBody()
                  .statement(sql)
                  .putExecutionConfigItem(
                      "pipeline.name", "Flink SQL Gateway SDK on flink cluster Example"));
      return statement1.getOperationHandle();
    } catch (ApiException e) {
      System.out.println(e.getResponseBody());
    }
    return null;
  }

  private static void runOnKubernetes(DefaultApi api) throws ApiException {
    OpenSessionResponseBody response =
        api.openSession(
            new OpenSessionRequestBody()
                .putPropertiesItem("kubernetes.cluster-id", "custom-flink-cluster")
                .putPropertiesItem("kubernetes.jobmanager.service-account", "flink")
                .putPropertiesItem("kubernetes.namespace", "flink-cluster")
                .putPropertiesItem("rest.address", "127.0.0.1")
                .putPropertiesItem("rest.port", "8081")
                .putPropertiesItem("execution.target", "kubernetes-session"));
    System.out.println(response.getSessionHandle());

    ExecuteStatementResponseBody statement1 =
        api.executeStatement(
            UUID.fromString(response.getSessionHandle()),
            new ExecuteStatementRequestBody()
                .statement(
                    "CREATE TABLE datagen (\n"
                        + " f_sequence INT,\n"
                        + " f_random INT,\n"
                        + " f_random_str STRING\n"
                        + ") WITH (\n"
                        + " 'connector' = 'datagen',\n"
                        + " 'rows-per-second'='10',\n"
                        + " 'fields.f_sequence.kind'='sequence',\n"
                        + " 'fields.f_sequence.start'='1',\n"
                        + " 'fields.f_sequence.end'='1000',\n"
                        + " 'fields.f_random.min'='1',\n"
                        + " 'fields.f_random.max'='1000',\n"
                        + " 'fields.f_random_str.length'='10'\n"
                        + ")")
                .putExecutionConfigItem("pipeline.name", "Flink SQL Gateway SDK on K8S Example"));

    System.out.println(statement1.getOperationHandle());

    ExecuteStatementResponseBody statement2 =
        api.executeStatement(
            UUID.fromString(response.getSessionHandle()),
            new ExecuteStatementRequestBody()
                .statement(
                    "CREATE TABLE blackhole_table  (\n"
                        + " f_sequence INT,\n"
                        + " f_random INT,\n"
                        + " f_random_str STRING\n"
                        + ") WITH (\n"
                        + " 'connector' = 'blackhole'\n"
                        + ")")
                .putExecutionConfigItem("pipeline.name", "Flink SQL Gateway SDK on K8S Example"));

    System.out.println(statement2.getOperationHandle());

    ExecuteStatementResponseBody statement3 =
        api.executeStatement(
            UUID.fromString(response.getSessionHandle()),
            new ExecuteStatementRequestBody()
                .statement(
                    "CREATE TABLE print_table  (\n"
                        + " f_sequence INT,\n"
                        + " f_random INT,\n"
                        + " f_random_str STRING\n"
                        + ") WITH (\n"
                        + " 'connector' = 'print'\n"
                        + ")")
                .putExecutionConfigItem("pipeline.name", "Flink SQL Gateway SDK on K8S Example"));

    System.out.println(statement3.getOperationHandle());

    ExecuteStatementResponseBody statement4 =
        api.executeStatement(
            UUID.fromString(response.getSessionHandle()),
            new ExecuteStatementRequestBody()
                .statement(
                    "EXECUTE STATEMENT SET\n"
                        + "BEGIN\n"
                        + "    insert into blackhole_table select * from datagen;\n"
                        + "    insert into print_table select * from datagen;\n"
                        + "END;")
                .putExecutionConfigItem("pipeline.name", "Flink SQL Gateway SDK on K8S Example"));

    System.out.println(statement4.getOperationHandle());
  }

  public static void runOnYarn(DefaultApi api) throws ApiException {
    OpenSessionResponseBody response =
        api.openSession(
            new OpenSessionRequestBody()
                .putPropertiesItem("execution.target", "yarn-session")
                .putPropertiesItem("flink.hadoop.yarn.resourcemanager.ha.enabled", "true")
                .putPropertiesItem("flink.hadoop.yarn.resourcemanager.ha.rm-ids", "rm1,rm2")
                .putPropertiesItem("flink.hadoop.yarn.resourcemanager.hostname.rm1", "yarn01")
                .putPropertiesItem("flink.hadoop.yarn.resourcemanager.hostname.rm2", "yarn01")
                .putPropertiesItem("flink.hadoop.yarn.resourcemanager.cluster-id", "yarn-cluster")
                .putPropertiesItem(
                    "flink.hadoop.yarn.client.failover-proxy-provider",
                    "org.apache.hadoop.yarn.client.ConfiguredRMFailoverProxyProvider")
                .putPropertiesItem("yarn.application.id", "application_1667789375191_XXXX"));
    System.out.println(response.getSessionHandle());
    ExecuteStatementResponseBody executeStatementResponseBody =
        api.executeStatement(
            UUID.fromString(response.getSessionHandle()),
            new ExecuteStatementRequestBody()
                .statement("select 1")
                .putExecutionConfigItem("pipeline.name", "Flink SQL Gateway SDK on YARN Example"));
    System.out.println(executeStatementResponseBody.getOperationHandle());
  }

  public static void runOnYarnWithUDF(DefaultApi api) throws ApiException {
    OpenSessionResponseBody response =
        api.openSession(
            new OpenSessionRequestBody()
                .putPropertiesItem("execution.target", "local")
                .putPropertiesItem("flink.hadoop.yarn.resourcemanager.ha.enabled", "true")
                .putPropertiesItem("flink.hadoop.yarn.resourcemanager.ha.rm-ids", "rm1,rm2")
                .putPropertiesItem("flink.hadoop.yarn.resourcemanager.hostname.rm1", "yarn01")
                .putPropertiesItem("flink.hadoop.yarn.resourcemanager.hostname.rm2", "yarn01")
                .putPropertiesItem("flink.hadoop.yarn.resourcemanager.cluster-id", "yarn-cluster")
                .putPropertiesItem(
                    "flink.hadoop.yarn.client.failover-proxy-provider",
                    "org.apache.hadoop.yarn.client.ConfiguredRMFailoverProxyProvider")
                .putPropertiesItem("yarn.application.id", "application_1667789375191_XXXX"));

    ExecuteStatementResponseBody statment1 =
        api.executeStatement(
            UUID.fromString(response.getSessionHandle()),
            new ExecuteStatementRequestBody()
                .statement(
                    "create TEMPORARY FUNCTION \n"
                        + "    FakeFunction as 'com.fortycoderplus.flink.udf.FakeFunction'\n"
                        + "using JAR 'hdfs://MyHdfsService/udf-test/fake-func.jar'")
                .putExecutionConfigItem("pipeline.name", "Flink SQL Gateway UDF on YARN Example"));
    System.out.println(statment1.getOperationHandle());

    ExecuteStatementResponseBody statment2 =
        api.executeStatement(
            UUID.fromString(response.getSessionHandle()),
            new ExecuteStatementRequestBody()
                .statement("select FakeFunction('Flink SQL Gateway UDF on YARN Example')")
                .putExecutionConfigItem(
                    "pipeline.name", "Flink SQL Gateway UDF on YARN Example-" + UUID.randomUUID()));
    System.out.println(statment2.getOperationHandle());
  }
}
