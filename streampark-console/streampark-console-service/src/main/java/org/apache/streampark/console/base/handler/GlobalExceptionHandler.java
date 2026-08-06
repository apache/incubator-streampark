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

package org.apache.streampark.console.base.handler;

import org.apache.streampark.common.util.ExceptionUtils;
import org.apache.streampark.console.base.domain.ResponseCode;
import org.apache.streampark.console.base.domain.RestResponseBody;
import org.apache.streampark.console.base.exception.AbstractApiException;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authz.UnauthenticatedException;

import com.baomidou.mybatisplus.core.toolkit.StringPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;

import java.util.List;
import java.util.Set;

@Slf4j
@RestControllerAdvice
@Order(value = Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    @ExceptionHandler(value = UnauthenticatedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public RestResponseBody<Void> handelUnauthenticatedException(UnauthenticatedException e) {
        log.error("Unauthenticated.", e);
        return RestResponseBody.fail(ResponseCode.CODE_UNAUTHORIZED, "Unauthenticated.");
    }

    @ExceptionHandler(value = AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public RestResponseBody<Void> handelUnauthenticatedException(AuthenticationException e) {
        log.error("Permission denied.", e);
        return RestResponseBody.fail(ResponseCode.CODE_UNAUTHORIZED, "Permission denied.");
    }

    @ExceptionHandler(value = AbstractApiException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestResponseBody<Void> handleException(AbstractApiException e) {
        log.error("api exception:", e);
        return RestResponseBody.fail(e.getResponseCode(), e.getMessage());
    }

    @ExceptionHandler(value = Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @Order(value = Ordered.HIGHEST_PRECEDENCE)
    public RestResponseBody<Void> handleException(Exception e) {
        log.error("internal server error:", e);
        return RestResponseBody.fail(
            ResponseCode.CODE_FAIL, "internal server error: " + ExceptionUtils.stringifyException(e));
    }

    private static String formatFieldErrors(List<FieldError> fieldErrors) {
        if (fieldErrors == null || fieldErrors.isEmpty()) {
            return "";
        }
        StringBuilder message = new StringBuilder();
        for (FieldError error : fieldErrors) {
            message.append(error.getField()).append(error.getDefaultMessage()).append(StringPool.COMMA);
        }
        return message.substring(0, message.length() - 1);
    }

    /**
     * Unified processing of request parameter verification (entity object parameter transfer)
     *
     * @param e BindException
     * @return RestResponse
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public RestResponseBody<Void> validExceptionHandler(BindException e) {
        log.error("bind exception:", e);
        return RestResponseBody.fail(ResponseCode.CODE_FAIL, formatFieldErrors(e.getBindingResult().getFieldErrors()));
    }

    /**
     * Unified processing of request parameter verification ({@code @RequestBody} JSON).
     *
     * @param e MethodArgumentNotValidException
     * @return RestResponseBody
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public RestResponseBody<Void> methodArgumentNotValidHandler(MethodArgumentNotValidException e) {
        log.error("method argument not valid exception:", e);
        return RestResponseBody.fail(ResponseCode.CODE_FAIL, formatFieldErrors(e.getBindingResult().getFieldErrors()));
    }

    /**
     * Unified processing of request parameter verification (ordinary parameter transfer)
     *
     * @param e ConstraintViolationException
     * @return RestResponseBody
     */
    @ExceptionHandler(value = ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public RestResponseBody<Void> handleConstraintViolationException(ConstraintViolationException e) {
        log.error("constraint violation exception:", e);
        StringBuilder message = new StringBuilder();
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        for (ConstraintViolation<?> violation : violations) {
            Path path = violation.getPropertyPath();
            String[] pathArr = StringUtils.splitByWholeSeparatorPreserveAllTokens(path.toString(), StringPool.DOT);
            String field = pathArr.length > 1 ? pathArr[pathArr.length - 1] : pathArr[0];
            message.append(field).append(violation.getMessage()).append(StringPool.COMMA);
        }
        message = new StringBuilder(message.substring(0, message.length() - 1));
        return RestResponseBody.fail(ResponseCode.CODE_FAIL, message.toString());
    }
}
