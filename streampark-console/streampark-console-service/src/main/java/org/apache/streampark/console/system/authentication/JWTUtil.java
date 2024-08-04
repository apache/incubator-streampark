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

import org.apache.streampark.console.base.util.EncryptUtils;
import org.apache.streampark.console.core.enums.AuthenticationType;
import org.apache.streampark.console.system.entity.User;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.regex.Pattern;

@Slf4j
public class JWTUtil {

    private static Long ttlOfSecond;

    private static final String JWT_USERID = "userId";
    private static final String JWT_USERNAME = "userName";
    private static final String JWT_TYPE = "type";
    private static final String JWT_TIMESTAMP = "timestamp";

    /**
     * verify token
     *
     * @param token token
     * @return is valid token
     */
    public static boolean verify(String token, String username, String secret) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            JWTVerifier verifier = JWT.require(algorithm).withClaim(JWT_USERNAME, username).build();
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
            return jwt.getClaim(JWT_USERNAME).asString();
        } catch (Exception ignored) {
            return null;
        }
    }

    public static Long getUserId(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getClaim(JWT_USERID).asLong();
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * @param token
     * @return
     */
    public static Long getTimestamp(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getClaim(JWT_TIMESTAMP).asLong();
        } catch (Exception ignored) {
            return 0L;
        }
    }

    /**
     * @param token
     * @return
     */
    public static AuthenticationType getAuthType(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            int type = jwt.getClaim(JWT_TYPE).asInt();
            return AuthenticationType.of(type);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * @param user
     * @param authType
     * @return
     * @throws Exception
     */
    public static String sign(User user, AuthenticationType authType) throws Exception {
        long second = getTTLOfSecond() * 1000;
        Long ttl = System.currentTimeMillis() + second;
        return sign(user, authType, ttl);
    }

    /**
     * @param user
     * @param authType
     * @param expireTime
     * @return
     * @throws Exception
     */
    public static String sign(User user, AuthenticationType authType, Long expireTime) throws Exception {
        Date date = new Date(expireTime);
        Algorithm algorithm = Algorithm.HMAC256(user.getPassword());

        JWTCreator.Builder builder =
            JWT.create()
                .withClaim(JWT_USERID, user.getUserId())
                .withClaim(JWT_USERNAME, user.getUsername())
                .withClaim(JWT_TYPE, authType.get())
                .withExpiresAt(date);

        if (authType == AuthenticationType.SIGN) {
            builder.withClaim(JWT_TIMESTAMP, System.currentTimeMillis());
        }

        String token = builder.sign(algorithm);
        return EncryptUtils.encrypt(token);
    }

    public static Long getTTLOfSecond() {
        if (ttlOfSecond == null) {
            String ttl = System.getProperty("server.session.ttl", "24h").trim();
            String regexp = "^\\d+([smhd])$";
            Pattern pattern = Pattern.compile(regexp);
            if (!pattern.matcher(ttl).matches()) {
                throw new IllegalArgumentException(
                    "server.session.ttl is invalid, Time units must be [s|m|h|d], e.g: 24h, 2d... please check config.yaml ");
            }
            String unit = ttl.substring(ttl.length() - 1);
            String time = ttl.substring(0, ttl.length() - 1);
            long second = Long.parseLong(time);
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
