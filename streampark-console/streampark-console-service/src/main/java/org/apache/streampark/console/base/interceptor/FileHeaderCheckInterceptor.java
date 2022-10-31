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

import org.apache.streampark.console.base.enums.FileType;
import org.apache.streampark.console.base.exception.IllegalFileTypeException;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component
public class FileHeaderCheckInterceptor implements HandlerInterceptor {

    private static List<String> fileHeaders = new ArrayList<>();
    private int headerLength = 8;

    static {
        fileHeaders.add(FileType.JAR.getMagicNumber());
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (request != null && request instanceof MultipartHttpServletRequest) {
            MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
            Map<String, MultipartFile> files = multipartRequest.getFileMap();
            Iterator<String> iterator = files.keySet().iterator();
            while (iterator.hasNext()) {
                String formKey = iterator.next();
                MultipartFile multipartFile = multipartRequest.getFile(formKey);
                byte[] file = multipartFile.getBytes();
                if (file.length > headerLength) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < headerLength; i++) {
                        int v = file[i] & 0xFF;
                        String hv = Integer.toHexString(v);
                        if (hv.length() < 2) {
                            sb.append(0);
                        }
                        sb.append(hv);
                    }
                    boolean isFound = false;
                    String fileHead = sb.toString().toUpperCase();
                    for (String header : fileHeaders) {
                        if (fileHead.startsWith(header)) {
                            isFound = true;
                            break;
                        }
                    }
                    if (!isFound) {
                        throw new IllegalFileTypeException("Illegal file type, please check");
                    }
                }
            }
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
