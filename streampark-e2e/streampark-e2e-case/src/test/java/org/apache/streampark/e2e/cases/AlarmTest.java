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

package org.apache.streampark.e2e.cases;

import org.apache.streampark.e2e.core.StreamParkApi;
import org.apache.streampark.e2e.core.api.ApiClient;
import org.apache.streampark.e2e.core.api.ApiResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@StreamParkApi(composeFiles = "docker/basic/docker-compose.yaml")
public class AlarmTest {

    public static ApiClient api;

    private static final String newEmail = "new@streampark.com";
    private static final String newAlarmName = "new_alarm";
    private static final String editAlarmName = "edit_alarm";

    private static Long alertId;

    @BeforeAll
    public static void setup() {
        api.login();
    }

    @Test
    @Order(1)
    public void testCreateAlarm() {
        ApiResponse response = api.postJson("/flink/alert/add", api.writeJson(buildAlertBody(newAlarmName, 23, true)));
        assertThat(response.isSuccess()).isTrue();

        ApiResponse list = api.postJson("/flink/alert/list", "{}");
        assertThat(list.isSuccess()).isTrue();
        assertThat(list.getData().toString()).contains(newAlarmName);
        assertThat(list.getData().toString()).contains(newEmail);
    }

    @Test
    @Order(2)
    public void testEditAlarm() {
        alertId = findAlertId(newAlarmName).orElseThrow();

        ApiResponse response =
            api.postJson("/flink/alert/update", api.writeJson(buildAlertBody(editAlarmName, 22, false)));
        assertThat(response.isSuccess()).isTrue();

        ApiResponse list = api.postJson("/flink/alert/list", "{}");
        assertThat(list.getData().toString()).contains(editAlarmName);
        assertThat(list.getData().toString()).doesNotContain(newEmail);
    }

    @Test
    @Order(3)
    public void testDeleteAlarm() {
        ApiResponse response = api.deleteForm("/flink/alert/delete", api.params("id", String.valueOf(alertId)));
        assertThat(response.isSuccess()).isTrue();

        ApiResponse list = api.postJson("/flink/alert/list", "{}");
        assertThat(list.getData().toString()).doesNotContain(editAlarmName);
    }

    private static Optional<Long> findAlertId(String alertName) {
        ApiResponse list = api.postJson("/flink/alert/list", "{}");
        if (list.getData() == null || !list.getData().isArray()) {
            return Optional.empty();
        }
        for (JsonNode item : list.getData()) {
            if (alertName.equals(item.path("alertName").asText())) {
                return Optional.of(item.path("id").asLong());
            }
        }
        return Optional.empty();
    }

    private static ObjectNode buildAlertBody(String alertName, int alertType, boolean withEmail) {
        ObjectNode body = api.objectNode();
        if (alertId != null) {
            body.put("id", alertId);
        }
        body.put("alertName", alertName);
        body.put("alertType", alertType);
        if (withEmail) {
            ObjectNode emailParams = body.putObject("emailParams");
            emailParams.putArray("contacts").add(newEmail);
        }
        ObjectNode dingTalkParams = body.putObject("dingTalkParams");
        dingTalkParams.put("alertDingURL", "");
        dingTalkParams.put("token", "dingTalkToken");
        dingTalkParams.put("secretEnable", true);
        dingTalkParams.put("secretToken", "dingTalkSecretToken");
        dingTalkParams.put("isAtAll", true);
        dingTalkParams.putArray("contacts").add("dingTalkUser");
        ObjectNode weComParams = body.putObject("weComParams");
        weComParams.put("token", "wechatToken");
        ObjectNode larkParams = body.putObject("larkParams");
        larkParams.put("token", "larkToken");
        larkParams.put("secretEnable", true);
        larkParams.put("secretToken", "larkSecretToken");
        larkParams.put("isAtAll", true);
        return body;
    }
}
