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

package org.apache.streampark.console.base.domain;

import org.slf4j.helpers.MessageFormatter;

import javax.validation.constraints.NotNull;

import java.util.HashMap;

/**
 * Legacy REST envelope based on a {@link HashMap}. Prefer {@link RestResponseBody} at controller
 * boundaries; this type remains for internal transitions and compatibility helpers.
 *
 * @deprecated Use {@link RestResponseBody} for controller return types.
 */
@Deprecated(since = "3.0.0")
@SuppressWarnings("java:S1133")
public class RestResponse extends HashMap<String, Object> {

    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_FAIL = "error";
    public static final String STATUS_KEY = "status";
    public static final String CODE_KEY = "code";
    public static final String MESSAGE_KEY = "message";
    public static final String DATA_KEY = "data";

    private static final long serialVersionUID = 1L;

    public static RestResponse success(Object data) {
        RestResponse resp = success();
        resp.put(DATA_KEY, data);
        return resp;
    }

    /**
     * Returns the {@code data} payload cast to the requested type.
     */
    @SuppressWarnings("unchecked")
    public <T> T getDataAs(Class<T> type) {
        Object data = get(DATA_KEY);
        if (data == null) {
            return null;
        }
        return type.cast(data);
    }

    /**
     * Wraps this response in a typed {@link RestResponseBody}.
     */
    public <T> RestResponseBody<T> asBody() {
        return RestResponseBody.from(this);
    }

    public static RestResponse success() {
        RestResponse resp = new RestResponse();
        resp.put(STATUS_KEY, STATUS_SUCCESS);
        resp.put(CODE_KEY, ResponseCode.CODE_SUCCESS);
        return resp;
    }

    public static RestResponse fail(Long code, String format, Object... args) {
        String message = MessageFormatter.arrayFormat(format, args).getMessage();
        RestResponse resp = new RestResponse();
        resp.put(STATUS_KEY, STATUS_FAIL);
        resp.put(MESSAGE_KEY, message);
        resp.put(CODE_KEY, code);
        resp.put(DATA_KEY, null);
        return resp;
    }

    public RestResponse message(String message) {
        this.put(MESSAGE_KEY, message);
        return this;
    }

    public RestResponse data(Object data) {
        this.put(DATA_KEY, data);
        return this;
    }

    @NotNull
    @Override
    public RestResponse put(String key, Object value) {
        super.put(key, value);
        return this;
    }
}
