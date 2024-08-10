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
import org.apache.streampark.console.base.enums.Status;

import org.bouncycastle.util.Arrays;

import java.text.MessageFormat;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    protected ApiAlertException(String message) {
        super(message, ResponseCode.CODE_FAIL_ALERT);
    }

    protected ApiAlertException(Throwable cause) {
        super(cause, ResponseCode.CODE_FAIL_ALERT);
    }

    protected ApiAlertException(String message, Throwable cause) {
        super(message, cause, ResponseCode.CODE_FAIL_ALERT);
    }

    public static void throwIfNull(Object object, Status status, Object... args) {
        if (Objects.isNull(object)) {
            throwException(status, args);
        }
    }

    public static void throwIfNotNull(Object object, Status status, Object... args) {
        if (Objects.nonNull(object)) {
            throwException(status, args);
        }
    }

    public static void throwIfFalse(boolean expression, Status status, Object... args) {
        throwIfTrue(!expression, status, args);
    }

    public static void throwIfTrue(boolean expression, Status status, Object... args) {
        if (expression) {
            throwException(status, args);
        }
    }

    private static Object[] processArgs(Object[] args) {
        if (!Arrays.isNullOrEmpty(args)) {
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                if (arg instanceof Status) {
                    args[i] = ((Status) arg).getMessage();
                }
            }
        }
        return args;
    }

    private static final Pattern MESSAGE_BRACES_PATTERN = Pattern.compile("\\{([^{}]+)}");

    private static String formatMessage(Status status, Object... args) {
        // Use regular expressions to find all the contents in the curly braces and that they are not pure numbers
        Matcher matcher = MESSAGE_BRACES_PATTERN.matcher(status.getMessage().replaceAll("'", "''"));
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String content = matcher.group(1);
            // if the content is not a pure number replace it
            if (!content.matches("\\d+")) {
                String replacement = "'{" + content + "}'";
                matcher.appendReplacement(result, replacement);
            } else {
                matcher.appendReplacement(result, "{" + content + "}");
            }
        }
        matcher.appendTail(result);
        return MessageFormat.format(result.toString(), processArgs(args));
    }

    public static <T> T throwException(Status status, Throwable cause, Object... args) {
        throw new ApiAlertException(formatMessage(status, args), cause);
    }

    public static <T> T throwException(Status status, Object... args) {
        throw new ApiAlertException(formatMessage(status, args));
    }
}
