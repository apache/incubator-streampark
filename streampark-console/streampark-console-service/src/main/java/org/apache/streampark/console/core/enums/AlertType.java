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
    email(1),
    dingTalk(2),
    weCom(4),
    httpCallback(8),
    lark(16);

    private final Integer code;
    private static Map<Integer, AlertType> cacheMap;

    AlertType(Integer code) {
        this.code = code;
    }

    /*
     * 报警方式，二进制位表示
     * 其中第 1 位表示:邮件报警，第 2 位表示: 钉钉报警，第 3 位表示: 企微报警，第 4 位表示: callback，第 5 位表示: 飞书。
     * 示例：
     * level= 3，其二进制位为：0000 0011， 则对应的报警方式位：钉钉，邮件
     * level= 10，其二进制位为：0000 1010， 则对应的报警方式位：钉钉，callback
     */
    public static List<AlertType> decode(Integer level) {
        if (level == null) {
            level = 0;
        }
        List<AlertType> result = Lists.newArrayList();
        while (level != 0) {
            // 获取最低位的 1
            int code = level & -level;
            result.add(getByCode(code));
            // 将最低位置 0
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

    public String getServiceType() {
        return this.name() + "AlertNotifyServiceImpl";
    }

    @JsonValue
    public int getCode() {
        return this.code;
    }
}
