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

import org.apache.streampark.console.SpringTestBase;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.Resource;
import org.apache.streampark.console.core.enums.EngineType;
import org.apache.streampark.console.core.enums.ResourceType;
import org.apache.streampark.console.core.enums.UserType;
import org.apache.streampark.console.core.service.application.ValidateApplicationService;
import org.apache.streampark.console.system.entity.User;
import org.apache.streampark.console.system.service.UserService;

import com.baomidou.mybatisplus.extension.toolkit.Db;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.Map;

/** org.apache.streampark.console.core.service.UserServiceTest. */
class UserServiceTest extends SpringTestBase {
  @Autowired private UserService userService;
  @Autowired private ValidateApplicationService validateApplicationService;
  @Autowired private ResourceService resourceService;

  @Test
  @SuppressWarnings("unchecked")
  void testLockUser() throws Exception {
    User user = new User();
    user.setUsername("test");
    user.setNickName("test");
    user.setPassword("test");
    user.setUserType(UserType.USER);
    user.setStatus(User.STATUS_VALID);
    Db.save(user);
    // lock user
    user.setStatus(User.STATUS_LOCK);
    Map<String, Object> data =
        (Map<String, Object>)
            userService.updateUser(user).getOrDefault("data", Collections.emptyMap());
    Assertions.assertNotEquals(true, data.get("needTransferResource"));
    // unlock user
    user.setStatus(User.STATUS_VALID);
    Map<String, Object> data1 =
        (Map<String, Object>)
            userService.updateUser(user).getOrDefault("data", Collections.emptyMap());
    Assertions.assertNotEquals(true, data1.get("needTransferResource"));

    Resource resource = new Resource();
    resource.setResourceName("test");
    resource.setResourceType(ResourceType.FLINK_APP);
    resource.setEngineType(EngineType.FLINK);
    resource.setTeamId(1L);
    resource.setCreatorId(user.getUserId());
    Db.save(resource);
    // lock user when has resource
    user.setStatus(User.STATUS_LOCK);
    Map<String, Object> data2 =
        (Map<String, Object>)
            userService.updateUser(user).getOrDefault("data", Collections.emptyMap());
    Assertions.assertEquals(true, data2.get("needTransferResource"));
  }

  @Test
  void testTransferResource() {
    User user = new User();
    user.setUsername("test");
    user.setNickName("test");
    user.setPassword("test");
    user.setUserType(UserType.USER);
    user.setStatus(User.STATUS_VALID);
    Db.save(user);

    Resource resource = new Resource();
    resource.setResourceName("test");
    resource.setResourceType(ResourceType.FLINK_APP);
    resource.setEngineType(EngineType.FLINK);
    resource.setTeamId(1L);
    resource.setCreatorId(user.getUserId());
    Db.save(resource);

    Application app = new Application();
    app.setUserId(user.getUserId());
    app.setTeamId(1L);
    Db.save(app);

    User targetUser = new User();
    targetUser.setUsername("test0");
    targetUser.setNickName("test0");
    targetUser.setPassword("test0");
    targetUser.setUserType(UserType.USER);
    targetUser.setStatus(User.STATUS_VALID);
    Db.save(targetUser);

    Assertions.assertTrue(validateApplicationService.existsByUserId(user.getUserId()));
    Assertions.assertTrue(resourceService.existsByUserId(user.getUserId()));

    userService.transferResource(user.getUserId(), targetUser.getUserId());

    Assertions.assertFalse(validateApplicationService.existsByUserId(user.getUserId()));
    Assertions.assertFalse(resourceService.existsByUserId(user.getUserId()));

    Assertions.assertTrue(validateApplicationService.existsByUserId(targetUser.getUserId()));
    Assertions.assertTrue(resourceService.existsByUserId(targetUser.getUserId()));
  }
}
