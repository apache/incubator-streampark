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

package org.apache.streampark.console.base.exception;

import org.apache.streampark.console.base.domain.ResponseCode;
import org.apache.streampark.console.base.enums.MessageStatus;

import java.text.MessageFormat;
import java.util.Objects;

/**
 * <pre>
 * To notify the frontend of an exception message,
 * it is usually a <strong> clear </strong> and <strong> concise </strong> message, e.g:
 * <p> 1. The username already exists.</p>
 * <p> 2. No permission, please contact the administrator.</p>
 * ...
 * </pre>
 */
public class ApiAlertException extends AbstractApiException {

    public ApiAlertException(String message) {
        super(message, ResponseCode.CODE_FAIL_ALERT);
    }

    public ApiAlertException(MessageStatus status, Object... args) {
        super(MessageFormat.format(status.getMsg(), args), ResponseCode.CODE_FAIL_ALERT);
    }

    public ApiAlertException(Throwable cause) {
        super(cause, ResponseCode.CODE_FAIL_ALERT);
    }

    public ApiAlertException(MessageStatus status, Throwable cause, Object... args) {
        super(MessageFormat.format(status.getMsg(), args), cause, ResponseCode.CODE_FAIL_ALERT);
    }

    public ApiAlertException(String message, Throwable cause) {
        super(message, cause, ResponseCode.CODE_FAIL_ALERT);
    }

    public static void throwIfNull(Object object, String errorMsgFmt, Object... args) {
        if (Objects.isNull(object)) {
            if (args == null || args.length < 1) {
                throw new ApiAlertException(errorMsgFmt);
            }
            throw new ApiAlertException(String.format(errorMsgFmt, args));
        }
    }

    public static void throwIfNull(Object object, MessageStatus status, Object... args) {
        if (Objects.isNull(object)) {
            throw new ApiAlertException(status, args);
        }
    }

    public static void throwIfNotNull(Object object, MessageStatus status, Object... args) {
        if (Objects.nonNull(object)) {
            throw new ApiAlertException(status, args);
        }
    }

    public static void throwIfFalse(boolean expression, String errorMessage) {
        if (!expression) {
            throw new ApiAlertException(errorMessage);
        }
    }

    public static void throwIfFalse(boolean expression, MessageStatus status, Object... args) {
        throwIfTrue(!expression, status, args);
    }

    public static void throwIfTrue(boolean expression, MessageStatus status, Object... args) {
        if (expression) {
            throw new ApiAlertException(status, args);
        }
    }
}
