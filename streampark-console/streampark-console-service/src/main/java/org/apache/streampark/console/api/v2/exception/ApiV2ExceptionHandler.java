/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.streampark.console.api.v2.exception;

import org.apache.streampark.console.api.controller.model.Error;
import org.apache.streampark.console.base.exception.AbstractApiException;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.UnauthorizedException;

import com.baomidou.mybatisplus.core.toolkit.StringPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;

import java.util.List;
import java.util.Set;

@Slf4j
@RestControllerAdvice(value = {"org.apache.streampark.console.api"})
@Order(value = Ordered.HIGHEST_PRECEDENCE)
public class ApiV2ExceptionHandler {

  @ExceptionHandler(value = Exception.class)
  public ResponseEntity<Error> handleException(Exception e, HttpServletRequest request) {
    log.error("Internal server error: ", e);
    return new ResponseEntity<>(
        Error.builder()
            .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .message(e.getMessage())
            .target(String.format("%s %s", request.getMethod(), request.getRequestURI()))
            .build(),
        HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(value = HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<Error> handleException(
      HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
    log.error("not supported request method，exception：{}", e.getMessage());
    return new ResponseEntity<>(
        Error.builder()
            .code(HttpStatus.METHOD_NOT_ALLOWED.value())
            .message(e.getMessage())
            .target(String.format("%s %s", request.getMethod(), request.getRequestURI()))
            .build(),
        HttpStatus.METHOD_NOT_ALLOWED);
  }

  @ExceptionHandler(value = AbstractApiException.class)
  public ResponseEntity<Error> handleException(AbstractApiException e, HttpServletRequest request) {
    log.error("ApiV1 Exception, {}", e.getMessage());
    return new ResponseEntity<>(
        Error.builder()
            .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .message(e.getMessage())
            .target(String.format("%s %s", request.getMethod(), request.getRequestURI()))
            .build(),
        HttpStatus.INTERNAL_SERVER_ERROR);
  }

  /**
   * Unified processing of request parameter verification (entity object parameter transfer)
   *
   * @param e BindException
   * @return RestResponse
   */
  @ExceptionHandler(BindException.class)
  public ResponseEntity<Error> validExceptionHandler(BindException e, HttpServletRequest request) {
    StringBuilder message = new StringBuilder();
    List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
    for (FieldError error : fieldErrors) {
      message.append(error.getField()).append(error.getDefaultMessage()).append(StringPool.COMMA);
    }
    message = new StringBuilder(message.substring(0, message.length() - 1));
    return handleBadRequestException(message.toString(), request);
  }

  /**
   * Unified processing of request parameter verification (ordinary parameter transfer)
   *
   * @param e ConstraintViolationException
   * @return RestResponse
   */
  @ExceptionHandler(value = ConstraintViolationException.class)
  public ResponseEntity<Error> handleConstraintViolationException(
      ConstraintViolationException e, HttpServletRequest request) {
    StringBuilder message = new StringBuilder();
    Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
    for (ConstraintViolation<?> violation : violations) {
      Path path = violation.getPropertyPath();
      String[] pathArr =
          StringUtils.splitByWholeSeparatorPreserveAllTokens(path.toString(), StringPool.DOT);
      message.append(pathArr[1]).append(violation.getMessage()).append(StringPool.COMMA);
    }
    message = new StringBuilder(message.substring(0, message.length() - 1));
    return handleBadRequestException(message.toString(), request);
  }

  private ResponseEntity<Error> handleBadRequestException(
      String message, HttpServletRequest request) {
    return new ResponseEntity<>(
        Error.builder()
            .code(HttpStatus.BAD_REQUEST.value())
            .message(message)
            .target(String.format("%s %s", request.getMethod(), request.getRequestURI()))
            .build(),
        HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(value = UnauthorizedException.class)
  public ResponseEntity<Error> handleUnauthorizedException(
      Exception e, HttpServletRequest request) {
    log.error("UnauthorizedException, {}", e.getMessage());
    return new ResponseEntity<>(
        Error.builder()
            .code(HttpStatus.UNAUTHORIZED.value())
            .message(e.getMessage())
            .target(String.format("%s %s", request.getMethod(), request.getRequestURI()))
            .build(),
        HttpStatus.UNAUTHORIZED);
  }

  /** Api V2 Exception */
  @ExceptionHandler(ApiV2Exception.class)
  public ResponseEntity<Error> handleErrorException(ApiV2Exception e, HttpServletRequest request) {
    log.error("ApiV2Exception, {}, {}", e.getCode(), e.getMessage());
    return new ResponseEntity<>(
        Error.builder()
            .code(e.getCode())
            .message(e.getMessage())
            .target(String.format("%s %s", request.getMethod(), request.getRequestURI()))
            .build(),
        e.getStatus());
  }
}
