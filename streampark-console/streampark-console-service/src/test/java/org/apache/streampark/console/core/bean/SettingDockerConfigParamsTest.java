/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.streampark.console.core.bean;

import org.apache.streampark.console.core.entity.Setting;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SettingDockerConfigParamsTest {
  private SettingDockerConfigParams dockerConfigParams;
  private Setting setting;
  private Method method;

  @BeforeEach
  void setUp() {
    dockerConfigParams = new SettingDockerConfigParams();
    setting = new Setting();
  }

  @Test
  void verifyUserNameTest()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    dockerConfigParams.setUsername(setting);
    initVerifyMethod("verifyUserName");

    setting.setSettingValue("Aa111111");
    Assertions.assertTrue((boolean) method.invoke(dockerConfigParams));

    setting.setSettingValue("Aa111111@");
    Assertions.assertFalse((boolean) method.invoke(dockerConfigParams));

    setting.setSettingValue("Aa@aa_");
    Assertions.assertFalse((boolean) method.invoke(dockerConfigParams));

    setting.setSettingValue("test");
    Assertions.assertFalse((boolean) method.invoke(dockerConfigParams));

    setting.setSettingValue("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee");
    Assertions.assertFalse((boolean) method.invoke(dockerConfigParams));

    setting.setSettingValue(null);
    Assertions.assertFalse((boolean) method.invoke(dockerConfigParams));
  }

  @Test
  void verifyPassWordTest()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    dockerConfigParams.setPassword(setting);
    initVerifyMethod("verifyPassWord");

    setting.setSettingValue("AaaAaa");
    Assertions.assertTrue((boolean) method.invoke(dockerConfigParams));

    setting.setSettingValue("AaAa");
    Assertions.assertFalse((boolean) method.invoke(dockerConfigParams));

    setting.setSettingValue("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee");
    Assertions.assertFalse((boolean) method.invoke(dockerConfigParams));

    setting.setSettingValue("123456789");
    Assertions.assertFalse((boolean) method.invoke(dockerConfigParams));

    setting.setSettingValue(null);
    Assertions.assertFalse((boolean) method.invoke(dockerConfigParams));
  }

  @Test
  void verifyAddressTest()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    dockerConfigParams.setAddress(setting);
    initVerifyMethod("verifyAddress");

    setting.setSettingValue("https://www.google.com/");
    Assertions.assertTrue((boolean) method.invoke(dockerConfigParams));

    setting.setSettingValue("http://www.google.com/");
    Assertions.assertTrue((boolean) method.invoke(dockerConfigParams));

    setting.setSettingValue("www.google.com");
    Assertions.assertFalse((boolean) method.invoke(dockerConfigParams));

    setting.setSettingValue("2001:0db8:85a3:0000:0000:8a2e:0370:7334");
    Assertions.assertTrue((boolean) method.invoke(dockerConfigParams));

    setting.setSettingValue("127.0.0.1");
    Assertions.assertTrue((boolean) method.invoke(dockerConfigParams));

    setting.setSettingValue("htp://www.google.com");
    Assertions.assertFalse((boolean) method.invoke(dockerConfigParams));

    setting.setSettingValue("ww.google.com");
    Assertions.assertFalse((boolean) method.invoke(dockerConfigParams));

    setting.setSettingValue("localhost");
    Assertions.assertFalse((boolean) method.invoke(dockerConfigParams));

    setting.setSettingValue("0.0.0");
    Assertions.assertFalse((boolean) method.invoke(dockerConfigParams));

    setting.setSettingValue(null);
    Assertions.assertFalse((boolean) method.invoke(dockerConfigParams));
  }

  @Test
  void verifyNameSpaceTest()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    dockerConfigParams.setAddress(setting);
    initVerifyMethod("verifyNameSpace");

    setting.setSettingValue("dom.mod");
    Assertions.assertFalse((boolean) method.invoke(dockerConfigParams));

    setting.setSettingValue(null);
    Assertions.assertFalse((boolean) method.invoke(dockerConfigParams));
  }

  void initVerifyMethod(final String methodName) throws NoSuchMethodException {
    Class<SettingDockerConfigParams> clazz = SettingDockerConfigParams.class;
    method = clazz.getDeclaredMethod(methodName);
    method.setAccessible(true);
  }
}
