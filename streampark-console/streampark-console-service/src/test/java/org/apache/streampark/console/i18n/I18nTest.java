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

package org.apache.streampark.console.i18n;

import org.apache.streampark.console.SpringUnitTestBase;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;

import java.util.Locale;

import static org.junit.Assert.assertEquals;

public class I18nTest extends SpringUnitTestBase {
  @Autowired private MessageSource messageSource;

  /** get english message from i18n resource */
  @Test
  public void readEnglishTest() {
    String msg = messageSource.getMessage("alertException.message.10020100", null, Locale.ENGLISH);
    assertEquals("Failed send DingTalk alert", msg);
  }

  /** get chinese message from i18n resource */
  @Test
  public void readChineseTest() {
    String msg = messageSource.getMessage("alertException.message.10020100", null, Locale.CHINESE);
    assertEquals("发送钉钉告警失败", msg);
  }
}
