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

package org.apache.streampark.console.base.enums;

public enum ResourceMessageStatus implements Status {

    RESOURCE_ALREADY_ERROR(10300, "the resource {0} already exists, please check.", "资源{0}已经存在，请检查"),
    RESOURCE_NAME_NULL_FAILED(10310, "The resource name cannot be null", "资源名不能为空"),
    RESOURCE_NOT_EXIST_ERROR(10320, "the resource {0} doesn't exists, please check.", "资源{0}不存在，请检查"),
    RESOURCE_STILL_USE_DELETE_ERROR(10330, "The resource is still in use, cannot be removed.", "资源仍在使用中，无法删除。"),
    RESOURCE_POM_JAR_EMPTY(10340, "Please add pom or jar resource.", "请添加pom或jar资源。"),
    RESOURCE_FLINK_APP_JAR_EMPTY_ERROR(10350, "Please upload jar for Flink App resource", "请上传 jar 以获取Flink App资源"),
    RESOURCE_MULTI_FILE_ERROR(10360, "Please do not add multi dependency at one time.", "请不要一次添加多个依赖项"),
    RESOURCE_NAME_MODIFY_ERROR(10370, "Please make sure the resource name is not changed.", "请确保未更改资源名称"),
    RESOURCE_FLINK_JAR_NULL(10380, "flink app jar must exist.", "Flink App Jar 必须存在"),

    ;
    private final int code;
    private final String enMsg;
    private final String zhMsg;

    ResourceMessageStatus(int code, String enMsg, String zhMsg) {
        this.code = code;
        this.enMsg = enMsg;
        this.zhMsg = zhMsg;
    }

    @Override
    public int getCode() {
        return this.code;
    }

    @Override
    public String getEnMsg() {
        return this.enMsg;
    }

    @Override
    public String getZhMsg() {
        return this.zhMsg;
    }
}
