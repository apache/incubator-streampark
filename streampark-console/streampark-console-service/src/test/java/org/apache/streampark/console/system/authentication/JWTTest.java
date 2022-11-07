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

package org.apache.streampark.console.system.authentication;

import org.apache.streampark.common.util.DateUtils;
import org.apache.streampark.console.system.entity.AccessToken;

import com.auth0.jwt.JWT;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

class JWTTest {

    @Test
    void getExpireTime() {
        String userName = "black";
        String secret = UUID.randomUUID().toString();
        String expireTime = AccessToken.DEFAULT_EXPIRE_TIME;
        String token = JWTUtil.sign(10000L, userName, secret, DateUtils.getTime(expireTime, DateUtils.fullFormat(), TimeZone.getDefault()));

        assert token != null;
        Date expiresAt = JWT.decode(token).getExpiresAt();
        String decodeExpireTime = DateUtils.format(expiresAt, DateUtils.fullFormat(), TimeZone.getDefault());
        Assertions.assertEquals(expireTime, decodeExpireTime);
    }
}
