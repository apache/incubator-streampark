/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import com.streamxhub.streamx.common.util.DateUtils;
import com.streamxhub.streamx.console.system.authentication.JWTUtil;
import com.streamxhub.streamx.console.system.entity.AccessToken;

import com.auth0.jwt.JWT;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

public class JWTTest {

    @Test
    public void getExpireTime() {
        String userName = "black";
        String secret = UUID.randomUUID().toString();
        String expireTime = AccessToken.DEFAULT_EXPIRE_TIME;
        String token = JWTUtil.sign(userName, secret, DateUtils.getTime(expireTime, DateUtils.fullFormat(), TimeZone.getDefault()));

        assert token != null;
        Date expiresAt = JWT.decode(token).getExpiresAt();
        String decodeExpireTime = DateUtils.format(expiresAt, DateUtils.fullFormat(), TimeZone.getDefault());
        Assert.assertEquals(expireTime, decodeExpireTime);
    }
}
