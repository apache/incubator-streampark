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

package org.apache.streampark.console.system.security.impl.ldap;

import org.apache.streampark.console.base.util.ShaHashUtils;
import org.apache.streampark.console.system.entity.User;
import org.apache.streampark.console.system.security.impl.AbstractAuthenticator;
import org.apache.streampark.console.system.security.impl.pwd.PasswordAuthenticator;
import org.apache.streampark.console.system.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

public class LdapAuthenticator extends AbstractAuthenticator {
    @Autowired
    private UserService usersService;
    @Autowired
    private LdapService ldapService;

    @Autowired
    private PasswordAuthenticator passwordAuthenticator;

    @Override
    public User login(String userId, String password) throws Exception {
        // admin login by username and password
        if ("admin".equals(userId)) {
            return passwordAuthenticator.login(userId, password);
        }
        String ldapUser = ldapService.ldapLogin(userId, password);
        // ldapUser is null, login by default
        if (ldapUser == null) {
            return passwordAuthenticator.login(userId, password);
        }
        //check if user exist
        User user = usersService.findByName(userId);
        if (user != null) {
            return passwordAuthenticator.login(userId, password);
        }
        // create ....
        User newUser = new User();
        newUser.setCreateTime(new Date());
        newUser.setUsername(userId);
        newUser.setNickName(userId);
        newUser.setStatus("1");
        newUser.setSex("1");

        String salt = ShaHashUtils.getRandomSalt();
        String saltPass = ShaHashUtils.encrypt(salt, password);
        newUser.setSalt(salt);
        newUser.setPassword(saltPass);
        usersService.createUser(newUser);
        return newUser;
    }
}
