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

package org.apache.streampark.console.core.service.alert;

import org.apache.streampark.console.SpringUnitTestBase;
import org.apache.streampark.console.core.entity.AlertConfig;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/** org.apache.streampark.console.core.service.alert.AlertConfigServiceTest. */
class AlertConfigServiceTest extends SpringUnitTestBase {
  @Autowired private AlertConfigService alertConfigService;

  @AfterEach
  void cleanTestRecordsInDatabase() {
    alertConfigService.remove(new QueryWrapper<>());
  }

  @Test
  void testUpdateLinkServiceById() {
    AlertConfig config = new AlertConfig();
    config.setUserId(1L);
    config.setAlertName("alertName");
    config.setAlertType(0);
    config.setEmailParams("emailParams");
    config.setDingTalkParams("dingTalkParams");
    config.setWeComParams("weComParams");
    config.setHttpCallbackParams("httpCallbackParams");
    config.setLarkParams("larkParams");
    alertConfigService.save(config);
    config.setUserId(null);
    config.setAlertName("updatedAlertName");
    config.setAlertType(null);
    config.setEmailParams("updatedEmailParams");
    config.setDingTalkParams("updatedDingTalkParams");
    config.setWeComParams("updatedWeComParams");
    config.setHttpCallbackParams("updatedHttpCallbackParams");
    config.setLarkParams("updatedLarkParams");
    alertConfigService.updateById(config);
    config = alertConfigService.getById(config.getId());

    assertThat(config.getUserId()).isEqualTo(1L);
    assertThat(config.getAlertName()).isEqualTo("updatedAlertName");
    assertThat(config.getAlertType()).isEqualTo(0);
    assertThat(config.getEmailParams()).isEqualTo("updatedEmailParams");
    assertThat(config.getDingTalkParams()).isEqualTo("updatedDingTalkParams");
    assertThat(config.getWeComParams()).isEqualTo("updatedWeComParams");
    assertThat(config.getHttpCallbackParams()).isEqualTo("updatedHttpCallbackParams");
    assertThat(config.getLarkParams()).isEqualTo("updatedLarkParams");
  }
}
