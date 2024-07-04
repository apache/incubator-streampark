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

import org.apache.streampark.console.core.enums.AuthenticationType;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.regex.Pattern;

@Slf4j
public class JWTUtil {

    private static Long ttlOfSecond;

    /**
     * verify token
     *
     * @param token token
     * @return is valid token
     */
    public static boolean verify(String token, String username, String secret) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            JWTVerifier verifier = JWT.require(algorithm).withClaim("userName", username).build();
            verifier.verify(token);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    /** get username from token */
    public static String getUserName(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getClaim("userName").asString();
        } catch (Exception ignored) {
            return null;
        }
    }

    public static Long getUserId(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getClaim("userId").asLong();
        } catch (Exception ignored) {
            return null;
        }
    }

    public static AuthenticationType getAuthType(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            int type = jwt.getClaim("type").asInt();
            return AuthenticationType.of(type);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * generate token
     *
     * @param userId
     * @param userName
     * @return
     */
    public static String sign(
                              Long userId, String userName, String secret, AuthenticationType authType) {
        Long second = getTTLOfSecond() * 1000;
        Long ttl = System.currentTimeMillis() + second;
        return sign(userId, userName, secret, authType, ttl);
    }

    /**
     * generate token
     *
     * @param userId
     * @param userName
     * @param expireTime
     * @return
     */
    public static String sign(
                              Long userId, String userName, String secret, AuthenticationType authType,
                              Long expireTime) {
        Date date = new Date(expireTime);
        Algorithm algorithm = Algorithm.HMAC256(secret);
        return JWT.create()
                .withClaim("userId", userId)
                .withClaim("userName", userName)
                .withClaim("type", authType.get())
                .withExpiresAt(date)
                .sign(algorithm);
    }

    public static Long getTTLOfSecond() {
        if (ttlOfSecond == null) {
            String ttl = System.getProperty("server.session.ttl", "24h").trim();
            String regexp = "^\\d+(s|m|h|d)$";
            Pattern pattern = Pattern.compile(regexp);
            if (!pattern.matcher(ttl).matches()) {
                throw new IllegalArgumentException(
                        "server.session.ttl is invalid, Time units must be [s|m|h|d], e.g: 24h, 2d... please check config.yaml ");
            }
            String unit = ttl.substring(ttl.length() - 1);
            String time = ttl.substring(0, ttl.length() - 1);
            Long second = Long.parseLong(time);
            switch (unit) {
                case "m":
                    return ttlOfSecond = second * 60;
                case "h":
                    return ttlOfSecond = second * 60 * 60;
                case "d":
                    return ttlOfSecond = second * 24 * 60 * 60;
                default:
                    return ttlOfSecond = second;
            }
        }
        return ttlOfSecond;
    }
}
