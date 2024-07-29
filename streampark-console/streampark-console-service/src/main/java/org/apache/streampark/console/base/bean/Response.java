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

package org.apache.streampark.console.base.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Response<T> {

    private Integer code;

    private String message;

    private T data;

    /**
     * return success.
     *
     * @return {@linkplain Response}
     */
    public static Response<Void> success() {
        return success(null);
    }

    /**
     * return success.
     *
     * @param data this is result data.
     * @return {@linkplain Response}
     */
    public static <T> Response<T> success(final T data) {
        return success(data, null);
    }

    /**
     * return success.
     *
     * @param message this ext msg.
     * @param data this is result data.
     * @return {@linkplain Response}
     */
    public static <T> Response<T> success(final T data, final String message) {
        return new Response<T>(ResponseCode.CODE_SUCCESS, message, data);
    }

    /**
     * return error.
     *
     * @param message error msg
     * @return {@linkplain Response}
     */
    public static Response<Void> fail(final String message) {
        return fail(message, ResponseCode.CODE_FAILED);
    }

    public static <T> Response<T> fail(final T data) {
        return fail(null, data, ResponseCode.CODE_FAILED);
    }

    public static <T> Response<T> fail(final int code) {
        return fail(null, null, code);
    }

    /**
     * return error.
     *
     * @param code error code
     * @param message error message
     * @return {@linkplain Response}
     */
    public static Response<Void> fail(final String message, final int code) {
        return fail(message, null, code);
    }

    public static <T> Response<T> fail(final String message, final T data) {
        return fail(message, data, ResponseCode.CODE_FAILED);
    }

    /**
     * return error.
     *
     * @param code error code
     * @param message error message
     * @param data this is result data.
     * @return {@linkplain Response}
     */
    public static <T> Response<T> fail(final String message, final T data, final int code) {
        return new Response<T>(code, message, data);
    }
}
