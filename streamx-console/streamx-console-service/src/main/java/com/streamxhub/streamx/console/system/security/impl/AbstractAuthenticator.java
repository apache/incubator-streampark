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

package com.streamxhub.streamx.console.system.security.impl;

import com.streamxhub.streamx.console.system.entity.User;
import com.streamxhub.streamx.console.system.security.Authenticator;

public abstract class AbstractAuthenticator implements Authenticator {

    /**
     * user login and return user in db
     *
     * @param userId user identity field
     * @param password user login password
     * @return user object in databse
     */
    public abstract User login(String userId, String password) throws Exception;

    @Override
    public User authenticate(String username, String password) throws Exception {
        return login(username, password);
    }
}
