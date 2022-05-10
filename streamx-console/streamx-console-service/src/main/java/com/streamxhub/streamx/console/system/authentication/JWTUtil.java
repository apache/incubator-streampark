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

package com.streamxhub.streamx.console.system.authentication;

import com.streamxhub.streamx.console.base.properties.ShiroProperties;
import com.streamxhub.streamx.console.base.util.SpringContextUtils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.AuthenticationException;

import java.util.Date;

/**
 * @author benjobs
 */
@Slf4j
public class JWTUtil {

    /**
     * 校验 token是否正确
     *
     * @param token  密钥
     * @param secret 用户的密码
     * @return 是否正确
     */
    public static boolean verify(String token, String username, String secret) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            JWTVerifier verifier = JWT.require(algorithm).withClaim("username", username).build();
            verifier.verify(token);
            return true;
        } catch (TokenExpiredException e) {
            throw new AuthenticationException(e.getMessage());
        } catch (Exception e) {
            log.info("token is invalid:{} , e:{}", e.getMessage(), e.getClass());
            return false;
        }
    }

    /**
     * 从 token中获取用户名
     *
     * @return token中包含的用户名
     */
    public static String getUsername(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getClaim("username").asString();
        } catch (JWTDecodeException e) {
            log.info("error：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 生成 token
     *
     * @param username 用户名
     * @param secret   用户的密码
     * @return token
     */
    public static String sign(String username, String secret) {
        return sign(username, secret, getExpireTime());
    }

    /**
     * 生成 token
     *
     * @param username     用户名
     * @param secret       用户的密码
     * @param expireTime   token过期时间
     * @return token
     */
    public static String sign(String username, String secret, Long expireTime) {
        try {
            username = StringUtils.lowerCase(username);
            Date date = new Date(expireTime);
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create().withClaim("username", username).withExpiresAt(date).sign(algorithm);
        } catch (Exception e) {
            log.info("error：{}", e);
            return null;
        }
    }

    /**
     * 获取用户登录token 失效时间
     */
    public static Long getExpireTime() {
        return System.currentTimeMillis() + SpringContextUtils.getBean(ShiroProperties.class).getJwtTimeOut() * 1000;
    }
}
