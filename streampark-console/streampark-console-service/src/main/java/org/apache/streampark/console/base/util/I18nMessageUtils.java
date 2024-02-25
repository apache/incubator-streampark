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

import org.apache.streampark.console.base.filter.LanguageHeaderFilter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;

import java.util.Locale;

/** Messages are returned in the language set by the user */
@Component
public class I18nMessageUtils {
  @Autowired private MessageSource messageSource;

  /**
   * build message for different language
   *
   * @param resourceKey the key of i18n resources
   * @param locale preferred language
   * @return message
   */
  @NotNull
  public String buildMessage(@NotNull String resourceKey, @NotNull Locale locale) {
    return messageSource.getMessage(resourceKey, null, locale);
  }

  /**
   * build message for different language
   *
   * @param i18nResourceKey the key of i18n resources
   * @return message
   */
  @NotNull
  public String message(@NotNull String i18nResourceKey) {
    Locale locale = new Locale(LanguageHeaderFilter.getLanguage());
    return buildMessage(i18nResourceKey, locale);
  }
}
