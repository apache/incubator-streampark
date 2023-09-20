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
import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.Resource;
import org.apache.streampark.console.core.enums.EngineTypeEnum;
import org.apache.streampark.console.core.enums.ResourceTypeEnum;
import org.apache.streampark.console.core.enums.UserTypeEnum;
import org.apache.streampark.console.core.service.application.ApplicationInfoService;
import org.apache.streampark.console.core.service.application.ApplicationManageService;
import org.apache.streampark.console.system.entity.User;
import org.apache.streampark.console.system.service.UserService;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;

/** org.apache.streampark.console.core.service.UserServiceTest. */
@Transactional
class UserServiceTest extends SpringUnitTestBase {
  @Autowired private UserService userService;
  @Autowired private ApplicationManageService applicationManageService;
  @Autowired private ApplicationInfoService applicationInfoService;
  @Autowired private ResourceService resourceService;

  @Test
  @SuppressWarnings("unchecked")
  void testLockUser() throws Exception {
    User user = new User();
    user.setUsername("test");
    user.setNickName("test");
    user.setPassword("test");
    user.setUserTypeEnum(UserTypeEnum.USER);
    user.setStatus(User.STATUS_VALID);
    userService.createUser(user);
    // lock user
    user.setStatus(User.STATUS_LOCK);
    Map<String, Object> data =
        (Map<String, Object>)
            userService
                .updateUser(user)
                .getOrDefault(RestResponse.DATA_KEY, Collections.emptyMap());
    Assertions.assertNotEquals(true, data.get("needTransferResource"));
    // unlock user
    user.setStatus(User.STATUS_VALID);
    Map<String, Object> data1 =
        (Map<String, Object>)
            userService
                .updateUser(user)
                .getOrDefault(RestResponse.DATA_KEY, Collections.emptyMap());
    Assertions.assertNotEquals(true, data1.get("needTransferResource"));

    Resource resource = new Resource();
    resource.setResourceName("test");
    resource.setResourceTypeEnum(ResourceTypeEnum.FLINK_APP);
    resource.setEngineTypeEnum(EngineTypeEnum.FLINK);
    resource.setTeamId(1L);
    resource.setCreatorId(user.getUserId());
    resourceService.save(resource);
    // lock user when has resource
    user.setStatus(User.STATUS_LOCK);
    Map<String, Object> data2 =
        (Map<String, Object>)
            userService
                .updateUser(user)
                .getOrDefault(RestResponse.DATA_KEY, Collections.emptyMap());
    Assertions.assertEquals(true, data2.get("needTransferResource"));
  }

  @Test
  void testTransferResource() {
    User user = new User();
    user.setUsername("test");
    user.setNickName("test");
    user.setPassword("test");
    user.setUserTypeEnum(UserTypeEnum.USER);
    user.setStatus(User.STATUS_VALID);
    userService.save(user);

    Resource resource = new Resource();
    resource.setResourceName("test");
    resource.setResourceTypeEnum(ResourceTypeEnum.FLINK_APP);
    resource.setEngineTypeEnum(EngineTypeEnum.FLINK);
    resource.setTeamId(1L);
    resource.setCreatorId(user.getUserId());
    resourceService.save(resource);

    Application app = new Application();
    app.setUserId(user.getUserId());
    app.setTeamId(1L);
    applicationManageService.save(app);

    User targetUser = new User();
    targetUser.setUsername("test0");
    targetUser.setNickName("test0");
    targetUser.setPassword("test0");
    targetUser.setUserTypeEnum(UserTypeEnum.USER);
    targetUser.setStatus(User.STATUS_VALID);
    userService.save(targetUser);

    Assertions.assertTrue(applicationInfoService.existsByUserId(user.getUserId()));
    Assertions.assertTrue(resourceService.existsByUserId(user.getUserId()));

    userService.transferResource(user.getUserId(), targetUser.getUserId());

    Assertions.assertFalse(applicationInfoService.existsByUserId(user.getUserId()));
    Assertions.assertFalse(resourceService.existsByUserId(user.getUserId()));

    Assertions.assertTrue(applicationInfoService.existsByUserId(targetUser.getUserId()));
    Assertions.assertTrue(resourceService.existsByUserId(targetUser.getUserId()));
  }
}
