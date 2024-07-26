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
public class RestResponse<T> {

    private Integer code;

    private String message;

    private T data;

    /**
    * return success.
    *
    * @return {@linkplain RestResponse}
    */
    public static RestResponse<Object> success() {
        return success("");
    }

    /**
    * return success.
    *
    * @param msg msg
    * @return {@linkplain RestResponse}
    */
    public static RestResponse<Object> success(final String msg) {
        return success(msg, null);
    }

    /**
    * return success.
    *
    * @param data this is result data.
    * @return {@linkplain RestResponse}
    */
    public static <T> RestResponse<T> success(final T data) {
        return success(null, data);
    }

    /**
    * return success.
    *
    * @param msg this ext msg.
    * @param data this is result data.
    * @return {@linkplain RestResponse}
    */
    public static <T> RestResponse<T> success(final String msg, final T data) {
        return new RestResponse<>(ResponseCode.CODE_SUCCESS, msg, data);
    }

    /**
    * return error.
    *
    * @param msg error msg
    * @return {@linkplain RestResponse}
    */
    public static RestResponse<Object> error(final String msg) {
        return error(ResponseCode.CODE_FAIL, msg);
    }

    /**
    * return error.
    *
    * @param code error code
    * @param msg error msg
    * @return {@linkplain RestResponse}
    */
    public static RestResponse<Object> error(final int code, final String msg) {
        return new RestResponse<>(code, msg, null);
    }

    /**
    * return error.
    *
    * @param code error code
    * @param msg error msg
    * @param data this is result data.
    * @return {@linkplain RestResponse}
    */
    public static <T> RestResponse<T> error(final int code, final String msg, final T data) {
        return new RestResponse<>(code, msg, data);
    }
}
