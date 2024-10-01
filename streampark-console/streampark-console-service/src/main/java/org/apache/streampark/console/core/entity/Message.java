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

package org.apache.streampark.console.core.entity;

import org.apache.streampark.console.core.enums.NoticeTypeEnum;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@TableName("t_message")
public class Message {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long appId;

    private Long userId;

    private String title;

    /** 1) build failure report 2) task monitoring exception */
    private NoticeTypeEnum type;

    private String context;

    private Boolean isRead;

    private Date createTime;

    public Message(
                   Long userId, Long appId, String title, String context, NoticeTypeEnum noticeTypeEnum) {
        this.userId = userId;
        this.appId = appId;
        this.title = title;
        this.context = context;
        this.type = noticeTypeEnum;
        this.createTime = new Date();
        this.isRead = false;
    }
}
