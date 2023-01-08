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

package org.apache.streampark.console.base.util;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class EncryptUtils {

  private static final String CIPHER_KEY = "AES";

  private static final String ALGORITHM = "AES/ECB/PKCS5Padding";

  private static final String DEFAULT_KEY = "ApacheStreamPark";

  public static String base64Encode(byte[] bytes) {
    return Base64.getEncoder().encodeToString(bytes);
  }

  public static byte[] base64Decode(String base64Code) {
    return Base64.getDecoder().decode(base64Code);
  }

  public static String encrypt(String content) throws Exception {
    return encrypt(content, DEFAULT_KEY);
  }

  public static String encrypt(String content, String key) throws Exception {
    String encryptKey = checkGetKey(key);
    KeyGenerator keyGenerator = KeyGenerator.getInstance(CIPHER_KEY);
    keyGenerator.init(128);
    Cipher cipher = Cipher.getInstance(ALGORITHM);
    cipher.init(
        Cipher.ENCRYPT_MODE,
        new SecretKeySpec(encryptKey.getBytes(StandardCharsets.UTF_8), CIPHER_KEY));
    byte[] bytes = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));
    return base64Encode(bytes);
  }

  public static String decrypt(String content) throws Exception {
    return decrypt(content, DEFAULT_KEY);
  }

  public static String decrypt(String content, String key) throws Exception {
    String decryptKey = checkGetKey(key);
    KeyGenerator keyGenerator = KeyGenerator.getInstance(CIPHER_KEY);
    keyGenerator.init(128);
    Cipher cipher = Cipher.getInstance(ALGORITHM);
    cipher.init(
        Cipher.DECRYPT_MODE,
        new SecretKeySpec(decryptKey.getBytes(StandardCharsets.UTF_8), CIPHER_KEY));
    byte[] decryptBytes = cipher.doFinal(base64Decode(content));
    return new String(decryptBytes, StandardCharsets.UTF_8);
  }

  private static String checkGetKey(String key) {
    if (key == null) {
      throw new IllegalArgumentException(
          "[StreamPark] EncryptUtils: key cannot be null, please check.");
    }
    if (key.trim().length() == 0) {
      throw new IllegalArgumentException(
          "[StreamPark] EncryptUtils: key cannot be empty, please check.");
    }
    if (key.length() == 16) {
      return key;
    }
    throw new IllegalArgumentException(
        "[StreamPark] EncryptUtils: the length of the key must be 16, please check.");
  }
}
