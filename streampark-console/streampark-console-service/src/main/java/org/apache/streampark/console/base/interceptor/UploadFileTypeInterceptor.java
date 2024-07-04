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

package org.apache.streampark.console.base.interceptor;

import org.apache.streampark.common.util.FileUtils;
import org.apache.streampark.console.base.exception.ApiAlertException;

import org.apache.tika.Tika;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.InputStream;
import java.util.Map;

/** An interceptor used to handle file uploads */
@Component
public class UploadFileTypeInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(UploadFileTypeInterceptor.class);

    @Override
    public boolean preHandle(
                             @Nonnull HttpServletRequest request,
                             @Nonnull HttpServletResponse response,
                             @Nonnull Object handler) throws Exception {
        if (request instanceof MultipartHttpServletRequest) {
            MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
            Map<String, MultipartFile> files = multipartRequest.getFileMap();
            for (String file : files.keySet()) {
                MultipartFile multipartFile = multipartRequest.getFile(file);
                ApiAlertException.throwIfNull(
                        multipartFile, "File to upload can't be null. Upload file failed.");
                InputStream input = multipartFile.getInputStream();
                boolean isJarOrPyFile = FileUtils.isJarFileType(input) || isPythonFile(input);
                ApiAlertException.throwIfFalse(
                        isJarOrPyFile,
                        "Illegal file type, Only support standard jar or python files. Upload file failed.");
            }
        }
        return true;
    }

    @Override
    public void postHandle(
                           @Nonnull HttpServletRequest request,
                           @Nonnull HttpServletResponse response,
                           @Nonnull Object handler,
                           ModelAndView modelAndView) throws Exception {
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    @Override
    public void afterCompletion(
                                @Nonnull HttpServletRequest request,
                                @Nonnull HttpServletResponse response,
                                @Nonnull Object handler,
                                Exception ex) throws Exception {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }

    private boolean isPythonFile(InputStream input) {
        try {
            Tika tika = new Tika();
            String mimeType = tika.detect(input);
            return mimeType.equals("text/x-python");
        } catch (Exception e) {
            logger.warn("check upload file type failed.", e);
            return false;
        }
    }
}
