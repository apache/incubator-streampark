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

public class SettingAlertEmailConfigParamsTest {
	private SettingAlertEmailConfigParams alertEmailConfigParams;
	private Setting setting;
	private Method method;
	
	@BeforeEach
	void setUp() {
		alertEmailConfigParams = new SettingAlertEmailConfigParams();
		setting = new Setting();
	}
	
	@Test
	void verifyEmailHostTest()
			throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		alertEmailConfigParams.setHost(setting);
		initVerifyMethod("verifyHost");
		
		setting.setSettingValue("test");
		Assertions.assertFalse((boolean) method.invoke(alertEmailConfigParams));
		
		setting.setSettingValue("testEmail@test.com");
		Assertions.assertTrue((boolean) method.invoke(alertEmailConfigParams));
		
		setting.setSettingValue("testEmail/test.com");
		Assertions.assertFalse((boolean) method.invoke(alertEmailConfigParams));
		
		setting.setSettingValue(null);
		Assertions.assertFalse((boolean) method.invoke(alertEmailConfigParams));
	}
	
	@Test
	void verifyEmailPortTest()
			throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		alertEmailConfigParams.setPort(setting);
		initVerifyMethod("verifyPort");
		
		setting.setSettingValue("3306");
		Assertions.assertTrue((boolean) method.invoke(alertEmailConfigParams));
		
		setting.setSettingValue("65535");
		Assertions.assertTrue((boolean) method.invoke(alertEmailConfigParams));
		
		setting.setSettingValue("-1");
		Assertions.assertFalse((boolean) method.invoke(alertEmailConfigParams));
		
		setting.setSettingValue("0");
		Assertions.assertFalse((boolean) method.invoke(alertEmailConfigParams));
		
		setting.setSettingValue("65536");
		Assertions.assertFalse((boolean) method.invoke(alertEmailConfigParams));
		
		setting.setSettingValue(null);
		Assertions.assertFalse((boolean) method.invoke(alertEmailConfigParams));
	}
	
	/* TODO .. */
	@Test
	void verifyFromTest() throws NoSuchMethodException {
		alertEmailConfigParams.setHost(setting);
		initVerifyMethod("verifyFrom");
	}
	
	@Test
	void verifyUserNameTest()
			throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		alertEmailConfigParams.setUsername(setting);
		initVerifyMethod("verifyUserName");
		
		
		setting.setSettingValue("Aa111111");
		Assertions.assertTrue((boolean) method.invoke(alertEmailConfigParams));
		
		setting.setSettingValue("Aa@aa_");
		Assertions.assertFalse((boolean) method.invoke(alertEmailConfigParams));
		
		setting.setSettingValue("test");
		Assertions.assertFalse((boolean) method.invoke(alertEmailConfigParams));
		
		setting.setSettingValue("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee");
		Assertions.assertFalse((boolean) method.invoke(alertEmailConfigParams));
		
		setting.setSettingValue(null);
		Assertions.assertFalse((boolean) method.invoke(alertEmailConfigParams));
	}
	
	@Test
	void verifyPassWordTest()
			throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		alertEmailConfigParams.setPassword(setting);
		initVerifyMethod("verifyPassWord");
		
		setting.setSettingValue("AaaAaa");
		Assertions.assertTrue((boolean) method.invoke(alertEmailConfigParams));
		
		setting.setSettingValue("AaAa");
		Assertions.assertFalse((boolean) method.invoke(alertEmailConfigParams));
		
		setting.setSettingValue("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee");
		Assertions.assertFalse((boolean) method.invoke(alertEmailConfigParams));
		
		setting.setSettingValue("123456789");
		Assertions.assertFalse((boolean) method.invoke(alertEmailConfigParams));
		
		setting.setSettingValue(null);
		Assertions.assertFalse((boolean) method.invoke(alertEmailConfigParams));
	}
	
	void initVerifyMethod(final String methodName) throws NoSuchMethodException {
		Class<SettingAlertEmailConfigParams> clazz = SettingAlertEmailConfigParams.class;
		method = clazz.getDeclaredMethod(methodName);
		method.setAccessible(true);
	}
}
