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

package org.apache.streampark.console.core.service;

import org.apache.streampark.console.SpringUnitTestBase;
import org.apache.streampark.console.core.entity.ExternalLink;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/** org.apache.streampark.console.core.service.ExternalLinkServiceTest. */
class ExternalLinkServiceTest extends SpringUnitTestBase {
  @Autowired private ExternalLinkService externalLinkService;

  @AfterEach
  void cleanTestRecordsInDatabase() {
    externalLinkService.remove(new QueryWrapper<>());
  }

  @Test
  void testUpdateLinkServiceById() {
    ExternalLink link = new ExternalLink();
    link.setBadgeLabel("badgeLabel");
    link.setBadgeColor("badgeColor");
    link.setBadgeName("badgeName");
    link.setLinkUrl("linkUrl");
    externalLinkService.save(link);
    link.setBadgeLabel("updatedBadgeLabel");
    link.setBadgeColor("updatedBadgeColor");
    link.setBadgeName("updatedBadgeName");
    link.setLinkUrl(null);
    externalLinkService.updateById(link);
    link = externalLinkService.getById(link.getId());

    assertThat(link.getBadgeLabel()).isEqualTo("updatedBadgeLabel");
    assertThat(link.getBadgeColor()).isEqualTo("updatedBadgeColor");
    assertThat(link.getBadgeName()).isEqualTo("updatedBadgeName");
    assertThat(link.getLinkUrl()).isEqualTo("linkUrl");
  }
}
