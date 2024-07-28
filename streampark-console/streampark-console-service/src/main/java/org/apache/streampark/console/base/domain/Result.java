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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private Integer code;

    private String message;

    private T data;

    private String status;

    /**
     * return success.
     *
     * @return {@linkplain Result}
     */
    public static Result<Void> success() {
        return success(null);
    }

    /**
     * return success.
     *
     * @param data this is result data.
     * @return {@linkplain Result}
     */
    public static <T> Result<T> success(final T data) {
        return success(data, null);
    }

    /**
     * return success.
     *
     * @param message this ext msg.
     * @param data this is result data.
     * @return {@linkplain Result}
     */
    public static <T> Result<T> success(final T data, final String message) {
        return new Result<T>(ResponseCode.CODE_SUCCESS, message, data, Constant.STATUS_SUCCESS);
    }

    /**
     * return error.
     *
     * @param message error msg
     * @return {@linkplain Result}
     */
    public static Result<Void> fail(final String message) {
        return fail(message, ResponseCode.CODE_FAILED);
    }

    public static <T> Result<T> fail(final T data) {
        return fail(null, data, ResponseCode.CODE_FAILED);
    }

    /**
     * return error.
     *
     * @param code error code
     * @param message error message
     * @return {@linkplain Result}
     */
    public static Result<Void> fail(final String message, final int code) {
        return fail(message, null, code);
    }

    public static <T> Result<T> fail(final String message, final T data) {
        return fail(message, data, ResponseCode.CODE_FAILED);
    }

    /**
     * return error.
     *
     * @param code error code
     * @param message error message
     * @param data this is result data.
     * @return {@linkplain Result}
     */
    public static <T> Result<T> fail(final String message, final T data, final int code) {
        return new Result<T>(code, message, data, Constant.STATUS_FAIL);
    }
}
