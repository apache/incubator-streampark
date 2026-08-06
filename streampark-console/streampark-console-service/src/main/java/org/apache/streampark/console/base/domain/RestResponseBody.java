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

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Typed view of {@link RestResponse} for safer access to the {@code data} payload.
 *
 * @param <T> deserialized data type
 */
@Getter
@Setter
public class RestResponseBody<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private String status;

    private Long code;

    private String message;

    private T data;

    @SuppressWarnings("unchecked")
    public static <T> RestResponseBody<T> from(RestResponse response) {
        RestResponseBody<T> body = new RestResponseBody<>();
        if (response == null) {
            return body;
        }
        body.setStatus((String) response.get(RestResponse.STATUS_KEY));
        body.setCode((Long) response.get(RestResponse.CODE_KEY));
        body.setMessage((String) response.get(RestResponse.MESSAGE_KEY));
        body.setData((T) response.get(RestResponse.DATA_KEY));
        return body;
    }

    public RestResponse toRestResponse() {
        RestResponse response = data != null ? RestResponse.success(data) : RestResponse.success();
        if (message != null) {
            response.message(message);
        }
        return response;
    }
}
