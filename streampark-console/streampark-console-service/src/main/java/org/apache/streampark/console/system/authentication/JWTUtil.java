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

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.shiro.authc.AuthenticationException;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

@Slf4j
public class JWTUtil {

  private static final long JWT_TIME_OUT =
      SpringContextUtils.getBean(ShiroProperties.class).getJwtTimeOut() * 1000;

  private static final Algorithm algorithm =
      Algorithm.HMAC256(RandomStringUtils.randomAlphanumeric(256));

  private static final String USER_NAME = "userName";

  private static final String USER_ID = "userId";

  /**
   * verify token
   *
   * @param token token
   * @return is valid token
   */
  public static boolean verify(String token, String username) {
    try {
      JWTVerifier verifier = JWT.require(algorithm).withClaim(USER_NAME, username).build();
      verifier.verify(token);
      return true;
    } catch (TokenExpiredException e) {
      throw new AuthenticationException(e.getMessage());
    } catch (Exception e) {
      log.error("token is invalid:{} , e:{}", e.getMessage(), e.getClass());
      return false;
    }
  }

  /** get username from token */
  public static String getUserName(String token) {
    try {
      DecodedJWT jwt = JWT.decode(token);
      return jwt.getClaim(USER_NAME).asString();
    } catch (JWTDecodeException e) {
      log.error("error：{}", e.getMessage());
      return null;
    }
  }

  public static Long getUserId(String token) {
    try {
      DecodedJWT jwt = JWT.decode(token);
      return jwt.getClaim(USER_ID).asLong();
    } catch (JWTDecodeException e) {
      log.error("error：{}", e.getMessage());
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
  public static String sign(Long userId, String userName) {
    return sign(userId, userName, getExpireTime());
  }

  /**
   * generate token
   *
   * @param userId
   * @param userName
   * @param expireTime
   * @return
   */
  public static String sign(Long userId, String userName, Long expireTime) {
    try {
      Date date = new Date(expireTime);
      return JWT.create()
          .withClaim(USER_ID, userId)
          .withClaim(USER_NAME, userName)
          .withExpiresAt(date)
          .sign(algorithm);
    } catch (Exception e) {
      log.error("error：{}", e.getMessage());
      return null;
    }
  }

  /** get token expire timestamp */
  private static Long getExpireTime() {
    return System.currentTimeMillis() + JWT_TIME_OUT;
  }
}
