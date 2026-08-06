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
import org.slf4j.helpers.MessageFormatter;

import java.io.Serializable;

/**
 * Typed REST envelope replacing raw {@link RestResponse} at controller boundaries.
 *
 * <p>Wire JSON shape is unchanged: {@code status}, {@code code}, optional {@code message}, optional
 * {@code data}.
 *
 * @param <T> payload type
 */
@Getter
@Setter
public class RestResponseBody<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private String status;

    private Long code;

    private String message;

    private T data;

    public static <T> RestResponseBody<T> success(T data) {
        RestResponseBody<T> body = new RestResponseBody<>();
        body.setStatus(RestResponse.STATUS_SUCCESS);
        body.setCode(ResponseCode.CODE_SUCCESS);
        body.setData(data);
        return body;
    }

    public static RestResponseBody<Void> success() {
        return success(null);
    }

    public static <T> RestResponseBody<T> fail(Long code, String format, Object... args) {
        String message = MessageFormatter.arrayFormat(format, args).getMessage();
        return fail(code, message);
    }

    public static <T> RestResponseBody<T> fail(Long code, String message) {
        RestResponseBody<T> body = new RestResponseBody<>();
        body.setStatus(RestResponse.STATUS_FAIL);
        body.setCode(code);
        body.setMessage(message);
        body.setData(null);
        return body;
    }

    public RestResponseBody<T> message(String message) {
        this.message = message;
        return this;
    }

    public RestResponseBody<T> data(T data) {
        this.data = data;
        return this;
    }

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
        RestResponse response = new RestResponse();
        response.put(RestResponse.STATUS_KEY, status);
        response.put(RestResponse.CODE_KEY, code);
        if (message != null) {
            response.put(RestResponse.MESSAGE_KEY, message);
        }
        response.put(RestResponse.DATA_KEY, data);
        return response;
    }
}
