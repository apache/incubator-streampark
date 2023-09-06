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
import org.apache.streampark.console.SpringUnitTestBase;
import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.Resource;
import org.apache.streampark.console.core.enums.EngineType;
import org.apache.streampark.console.core.enums.ResourceType;
import org.apache.streampark.console.core.enums.UserType;
import org.apache.streampark.console.system.entity.User;
import org.apache.streampark.console.system.service.UserService;

import com.baomidou.mybatisplus.extension.toolkit.Db;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** org.apache.streampark.console.core.service.UserServiceTest. */
@Transactional
class UserServiceTest extends SpringUnitTestBase {
    @Autowired private UserService userService;

    @Test
    void testLockUser() {
        User user = new User();
        user.setUsername("test");
        user.setNickName("test");
        user.setPassword("test");
        user.setUserType(UserType.USER);
        user.setStatus(User.STATUS_VALID);
        Db.save(user);
        // lock user
        Assertions.assertFalse(userService.lockUser(user.getUserId(), null));
        Assertions.assertEquals(User.STATUS_LOCK, Db.getById(user.getUserId(), User.class).getStatus());
        // unlock user
        userService.unlockUser(user.getUserId());

        Resource resource = new Resource();
        resource.setResourceName("test");
        resource.setResourceType(ResourceType.FLINK_APP);
        resource.setEngineType(EngineType.FLINK);
        resource.setTeamId(1L);
        resource.setCreatorId(user.getUserId());
        Db.save(resource);
        // lock user when has resource
        Assertions.assertTrue(userService.lockUser(user.getUserId(), null));
        // transferToUserId is null so not lock
        Assertions.assertEquals(
            User.STATUS_VALID, Db.getById(user.getUserId(), User.class).getStatus());

        // transfer resources
        Assertions.assertFalse(userService.lockUser(user.getUserId(), 100001L));
        Assertions.assertEquals(User.STATUS_LOCK, Db.getById(user.getUserId(), User.class).getStatus());
    }
}
