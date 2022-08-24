/*
 * Copyright 2019 The StreamX Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.console.base.handler;

import com.streamxhub.streamx.console.base.domain.ResponseCode;
import com.streamxhub.streamx.console.base.domain.RestResponse;
import com.streamxhub.streamx.console.base.exception.ApiException;
import com.streamxhub.streamx.console.base.exception.InternalException;

import com.baomidou.mybatisplus.core.toolkit.StringPool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.UnauthorizedException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
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

    @ExceptionHandler(value = Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestResponse handleException(Exception e) {
        log.info("Internal server error：", e);
        return RestResponse.fail("internal server error: " + e.getMessage(), ResponseCode.CODE_FAIL);
    }

    @ExceptionHandler(value = InternalException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestResponse handleParamsInvalidException(InternalException e) {
        log.info("Internal server error：{}", e.getMessage());
        return RestResponse.fail("internal server error: " + e.getMessage(), ResponseCode.CODE_FAIL);
    }

    @ExceptionHandler(value = HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestResponse handleException(HttpRequestMethodNotSupportedException e) {
        log.info("not supported request method，exception：{}", e.getMessage());
        return RestResponse.fail("not supported request method，exception：" + e.getMessage(), ResponseCode.CODE_FAIL);
    }

    @ExceptionHandler(value = ApiException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestResponse handleException(ApiException e) {
        log.info("api exception：{}", e.getMessage());
        return RestResponse.fail("api fail, exception:\n" + e.getMessage(), ResponseCode.CODE_API_FAIL);
    }

    /**
     * 统一处理请求参数校验(实体对象传参)
     *
     * @param e BindException
     * @return RestResponse
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public RestResponse validExceptionHandler(BindException e) {
        StringBuilder message = new StringBuilder();
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        for (FieldError error : fieldErrors) {
            message.append(error.getField()).append(error.getDefaultMessage()).append(StringPool.COMMA);
        }
        message = new StringBuilder(message.substring(0, message.length() - 1));
        return RestResponse.fail(message.toString(), ResponseCode.CODE_FAIL);
    }

    /**
     * 统一处理请求参数校验(普通传参)
     *
     * @param e ConstraintViolationException
     * @return RestResponse
     */
    @ExceptionHandler(value = ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public RestResponse handleConstraintViolationException(ConstraintViolationException e) {
        StringBuilder message = new StringBuilder();
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        for (ConstraintViolation<?> violation : violations) {
            Path path = violation.getPropertyPath();
            String[] pathArr = StringUtils.splitByWholeSeparatorPreserveAllTokens(path.toString(), StringPool.DOT);
            message.append(pathArr[1]).append(violation.getMessage()).append(StringPool.COMMA);
        }
        message = new StringBuilder(message.substring(0, message.length() - 1));
        return RestResponse.fail(message.toString(), ResponseCode.CODE_FAIL);
    }

    @ExceptionHandler(value = UnauthorizedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public void handleUnauthorizedException(Exception e) {
        log.info("Permission denied，{}", e.getMessage());
    }
}
