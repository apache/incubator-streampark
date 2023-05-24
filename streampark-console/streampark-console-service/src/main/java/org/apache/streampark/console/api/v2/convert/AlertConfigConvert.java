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

package org.apache.streampark.console.api.v2.convert;

import org.apache.streampark.console.api.controller.model.AlertConfigInfo;
import org.apache.streampark.console.api.controller.model.CreateAlertConfigRequest;
import org.apache.streampark.console.api.controller.model.DingTalk;
import org.apache.streampark.console.api.controller.model.Email;
import org.apache.streampark.console.api.controller.model.HttpCallback;
import org.apache.streampark.console.api.controller.model.Lark;
import org.apache.streampark.console.api.controller.model.UpdateAlertConfigRequest;
import org.apache.streampark.console.api.controller.model.WeCom;
import org.apache.streampark.console.base.util.JacksonUtils;
import org.apache.streampark.console.core.entity.AlertConfig;

import org.apache.commons.lang3.StringUtils;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Objects;

@Mapper(componentModel = "spring")
public interface AlertConfigConvert {

  @Mapping(source = "emailParams", target = "email", qualifiedByName = "toEmail")
  @Mapping(source = "dingTalkParams", target = "dingTalk", qualifiedByName = "toDingTalk")
  @Mapping(source = "weComParams", target = "weCom", qualifiedByName = "toWeCom")
  @Mapping(
      source = "httpCallbackParams",
      target = "httpCallback",
      qualifiedByName = "toHttpCallback")
  @Mapping(source = "larkParams", target = "lark", qualifiedByName = "toLark")
  AlertConfigInfo toVo(AlertConfig config);

  @Mapping(source = "email", target = "emailParams", qualifiedByName = "toJson")
  @Mapping(source = "dingTalk", target = "dingTalkParams", qualifiedByName = "toJson")
  @Mapping(source = "weCom", target = "weComParams", qualifiedByName = "toJson")
  @Mapping(source = "httpCallback", target = "httpCallbackParams", qualifiedByName = "toJson")
  @Mapping(source = "lark", target = "larkParams", qualifiedByName = "toJson")
  AlertConfig toPo(CreateAlertConfigRequest request);

  @Mapping(source = "email", target = "emailParams", qualifiedByName = "toJson")
  @Mapping(source = "dingTalk", target = "dingTalkParams", qualifiedByName = "toJson")
  @Mapping(source = "weCom", target = "weComParams", qualifiedByName = "toJson")
  @Mapping(source = "httpCallback", target = "httpCallbackParams", qualifiedByName = "toJson")
  @Mapping(source = "lark", target = "larkParams", qualifiedByName = "toJson")
  AlertConfig toPo(UpdateAlertConfigRequest request);

  List<AlertConfigInfo> toVo(List<AlertConfig> configs);

  @Named("toJson")
  static String toJson(Object object) {
    if (Objects.isNull(object)) {
      return null;
    }
    return JacksonUtils.writeIgnored(object);
  }

  @Named("toLark")
  static Lark toLark(String json) {
    if (StringUtils.isEmpty(json)) {
      return null;
    }
    return JacksonUtils.readIgnored(json, Lark.class);
  }

  @Named("toEmail")
  static Email toEmail(String json) {
    if (StringUtils.isEmpty(json)) {
      return null;
    }
    return JacksonUtils.readIgnored(json, Email.class);
  }

  @Named("toHttpCallback")
  static HttpCallback toHttpCallback(String json) {
    if (StringUtils.isEmpty(json)) {
      return null;
    }
    return JacksonUtils.readIgnored(json, HttpCallback.class);
  }

  @Named("toDingTalk")
  static DingTalk toDingTalk(String json) {
    if (StringUtils.isEmpty(json)) {
      return null;
    }
    return JacksonUtils.readIgnored(json, DingTalk.class);
  }

  @Named("toWeCom")
  static WeCom toWeCom(String json) {
    if (StringUtils.isEmpty(json)) {
      return null;
    }
    return JacksonUtils.readIgnored(json, WeCom.class);
  }
}
