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
package org.apache.streampark.console.core.enums;

import org.apache.streampark.console.core.service.alert.AlertNotifyService;
import org.apache.streampark.console.core.service.alert.impl.DingTalkAlertNotifyServiceImpl;
import org.apache.streampark.console.core.service.alert.impl.EmailAlertNotifyServiceImpl;
import org.apache.streampark.console.core.service.alert.impl.HttpCallbackAlertNotifyServiceImpl;
import org.apache.streampark.console.core.service.alert.impl.LarkAlertNotifyServiceImpl;
import org.apache.streampark.console.core.service.alert.impl.WeComAlertNotifyServiceImpl;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The AlertType enum represents different types of alerts that can be used for notifications. */
@Getter
public enum AlertTypeEnum {

    /** Email */
    EMAIL(1, EmailAlertNotifyServiceImpl.class),

    /** Ding talk */
    DING_TALK(2, DingTalkAlertNotifyServiceImpl.class),

    /** WeChat work */
    WE_COM(4, WeComAlertNotifyServiceImpl.class),

    /** Http callback */
    HTTP_CALLBACK(8, HttpCallbackAlertNotifyServiceImpl.class),

    /** Lark */
    LARK(16, LarkAlertNotifyServiceImpl.class);

    /** The empty level */
    private static final Integer EMPTY_LEVEL = 0;

    /** Get the alert type by the code */
    @JsonValue
    private final Integer code;

    /** Holds the reference to a Class object. */
    private final Class<? extends AlertNotifyService> clazz;

    /** A cache map used to quickly get the alert type from an integer code */
    private static final Map<Integer, AlertTypeEnum> CACHE_MAP = createCacheMap();

    private static Map<Integer, AlertTypeEnum> createCacheMap() {
        Map<Integer, AlertTypeEnum> cacheMap = new HashMap<>();
        for (AlertTypeEnum notifyType : AlertTypeEnum.values()) {
            cacheMap.put(notifyType.code, notifyType);
        }
        return Collections.unmodifiableMap(cacheMap);
    }

    AlertTypeEnum(Integer code, Class<? extends AlertNotifyService> clazz) {
        this.code = code;
        this.clazz = clazz;
    }

    public static List<AlertTypeEnum> decode(Integer level) {
        if (level == null) {
            level = EMPTY_LEVEL;
        }

        List<AlertTypeEnum> result = new ArrayList<>(AlertTypeEnum.values().length);
        while (level != 0) {
            int code = level & -level;
            result.add(getByCode(code));
            level ^= code;
        }
        return result;
    }

    public static int encode(List<AlertTypeEnum> alertTypeEnums) {
        if (CollectionUtils.isEmpty(alertTypeEnums)) {
            return EMPTY_LEVEL;
        }

        int result = 0;
        for (AlertTypeEnum alertTypeEnum : alertTypeEnums) {
            result |= alertTypeEnum.code;
        }
        return result;
    }

    private static AlertTypeEnum getByCode(Integer code) {
        return CACHE_MAP.get(code);
    }
}
