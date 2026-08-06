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

package org.apache.streampark.console.core.response.message;

import org.apache.streampark.console.core.enums.NoticeTypeEnum;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/** API response for a message, aligned with webapp {@code NotifyItem}. */
@Getter
@Setter
public class MessageResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long appId;

    private Long userId;

    private String title;

    private NoticeTypeEnum type;

    private String context;

    private Boolean isRead;

    private Date createTime;
}
