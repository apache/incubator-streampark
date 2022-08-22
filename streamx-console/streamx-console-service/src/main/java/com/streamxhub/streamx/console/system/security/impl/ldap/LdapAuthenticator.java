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

import com.streamxhub.streamx.console.system.entity.User;
import com.streamxhub.streamx.console.system.security.impl.AbstractAuthenticator;
import com.streamxhub.streamx.console.system.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;

public class LdapAuthenticator extends AbstractAuthenticator {
    @Autowired
    private UserService usersService;
    @Autowired
    LdapService ldapService;

    @Override
    public User login(String userId, String password) throws Exception {
        User user = null;
        String ldapEmail = ldapService.ldapLogin(userId, password);

        if (userId.equals("admin")) {
            user = usersService.findByName(userId);
            return user;
        }

        if (ldapEmail != null) {
            //check if user exist
            user = usersService.findByName(userId);
            if (user == null) {
                User newUser = new User();
                newUser.setUsername(userId);
                newUser.setRoleId("100001");
                newUser.setNickName(userId);
                newUser.setPassword(password);
                newUser.setStatus("1");
                newUser.setSex("1");
                usersService.createUser(newUser);
                return newUser;
            }
        }
        return user;
    }
}
