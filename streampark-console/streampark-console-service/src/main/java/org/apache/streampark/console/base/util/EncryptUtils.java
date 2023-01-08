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

import org.apache.commons.codec.digest.DigestUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class EncryptUtils {

  private static final String DEFAULT_KEY = DigestUtils.md5Hex("ApacheStreamPark");

  private static final int offset = 16;

  private static final String ALGORITHM = "AES";

  private static final String CIPHER_KEY = "AES/CBC/PKCS5Padding";

  public static String encrypt(String content) throws Exception {
    return encrypt(content, DEFAULT_KEY);
  }

  public static String decrypt(String content) throws Exception {
    return decrypt(content, DEFAULT_KEY);
  }

  public static String encrypt(String content, String key) throws Exception {
    SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), ALGORITHM);
    IvParameterSpec ivSpec = new IvParameterSpec(key.getBytes(), 0, offset);
    Cipher cipher = Cipher.getInstance(CIPHER_KEY);
    byte[] byteContent = content.getBytes(StandardCharsets.UTF_8);
    cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
    byte[] result = cipher.doFinal(byteContent);
    return Base64.getEncoder().encodeToString(result);
  }

  public static String decrypt(String content, String key) throws Exception {
    SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), ALGORITHM);
    IvParameterSpec ivSpec = new IvParameterSpec(key.getBytes(), 0, offset);
    Cipher cipher = Cipher.getInstance(CIPHER_KEY);
    cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
    byte[] result = cipher.doFinal(Base64.getDecoder().decode(content));
    return new String(result);
  }
}
