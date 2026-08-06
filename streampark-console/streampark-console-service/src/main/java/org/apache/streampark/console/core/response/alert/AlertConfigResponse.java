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

package org.apache.streampark.console.core.response.alert;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/** API response for an alert config, aligned with webapp {@code AlertSetting}. */
@Getter
@Setter
public class AlertConfigResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    private String alertName;

    private Integer alertType;

    /** JSON string, aligned with legacy {@code AlertConfig} wire format. */
    private String emailParams;

    /** JSON string, aligned with legacy {@code AlertConfig} wire format. */
    private String dingTalkParams;

    /** JSON string, aligned with legacy {@code AlertConfig} wire format. */
    private String weComParams;

    /** JSON string, aligned with legacy {@code AlertConfig} wire format. */
    private String httpCallbackParams;

    /** JSON string, aligned with legacy {@code AlertConfig} wire format. */
    private String larkParams;

    private Date createTime;

    private Date modifyTime;
}
