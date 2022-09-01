/*
 * Copyright 2019 The StreamX Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.console.system.security.impl.ldap;

import com.streamxhub.streamx.console.base.util.ShaHashUtils;
import com.streamxhub.streamx.console.system.entity.User;
import com.streamxhub.streamx.console.system.security.impl.AbstractAuthenticator;
import com.streamxhub.streamx.console.system.security.impl.pwd.PasswordAuthenticator;
import com.streamxhub.streamx.console.system.service.UserService;

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
        } else {
            //check if user exist
            User user = usersService.findByName(userId);
            if (user != null) {
                return passwordAuthenticator.login(userId, password);
            } else {
                // create ....
                User newUser = new User();
                newUser.setCreateTime(new Date());
                newUser.setUsername(userId);
                newUser.setRoleId("100001");
                newUser.setNickName(userId);
                newUser.setStatus("1");
                newUser.setSex("1");

                String salt = ShaHashUtils.getRandomSalt(26);
                String saltPass = ShaHashUtils.encrypt(salt, user.getPassword());
                newUser.setSalt(salt);
                newUser.setPassword(saltPass);
                usersService.createUser(newUser);
                return newUser;
            }
        }
    }
}
