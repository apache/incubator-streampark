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

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.Lists;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum AlertType {
    EMAIL(1),
    DING_TALK(2),
    WE_COM(4),
    HTTP_CALLBACK(8),
    LARK(16);

    private final Integer code;

    private static Map<Integer, AlertType> cacheMap;

    AlertType(Integer code) {
        this.code = code;
    }

    /**
     * Alarm way, binary bits indicate
     * Where bit 1 means: email alarm, bit 2 means: ding alarm, bit 3 means: weCom alarm, bit 4 means: http callback, and bit 5 means: lark.
     * Example:
     * level= 3, its binary bit is: 0000 0011, then the corresponding alarm mode bit: ding, email
     * level= 10, its binary bit is: 0000 1010, then the corresponding alarm mode bit: ding, callback
     */
    public static List<AlertType> decode(Integer level) {
        if (level == null) {
            level = 0;
        }
        List<AlertType> result = Lists.newArrayList();
        while (level != 0) {
            int code = level & -level;
            result.add(getByCode(code));
            level ^= code;
        }
        return result;
    }

    public static int encode(List<AlertType> alertTypes) {
        int result = 0;
        if (!CollectionUtils.isEmpty(alertTypes)) {
            for (AlertType alertType : alertTypes) {
                result |= alertType.code;
            }
        }
        return result;
    }

    private static AlertType getByCode(Integer code) {
        if (cacheMap == null) {
            cacheMap = new HashMap<>();
            for (AlertType notifyType : AlertType.values()) {
                cacheMap.put(notifyType.code, notifyType);
            }
        }
        return cacheMap.get(code);
    }

    @JsonValue
    public int getCode() {
        return this.code;
    }

}
