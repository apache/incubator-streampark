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

import org.apache.streampark.console.base.properties.ShiroProperties;
import org.apache.streampark.console.base.util.SpringContextUtils;
import org.apache.streampark.console.core.enums.AuthenticationType;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

@Slf4j
public class JWTUtil {

  private static final long JWT_TIME_OUT =
      SpringContextUtils.getBean(ShiroProperties.class).getJwtTimeOut() * 1000;

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
    return sign(userId, userName, secret, authType, getExpireTime());
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
      Long userId, String userName, String secret, AuthenticationType authType, Long expireTime) {
    Date date = new Date(expireTime);
    Algorithm algorithm = Algorithm.HMAC256(secret);
    return JWT.create()
        .withClaim("userId", userId)
        .withClaim("userName", userName)
        .withClaim("type", authType.get())
        .withExpiresAt(date)
        .sign(algorithm);
  }

  /** get token expire timestamp */
  private static Long getExpireTime() {
    return System.currentTimeMillis() + JWT_TIME_OUT;
  }
}
