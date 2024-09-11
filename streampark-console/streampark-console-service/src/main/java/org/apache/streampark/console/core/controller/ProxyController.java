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

package org.apache.streampark.console.core.controller;

import org.apache.streampark.console.core.service.ProxyService;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@Validated
@RestController
@RequestMapping("proxy")
public class ProxyController {

    @Autowired
    private ProxyService proxyService;

    @GetMapping("{type}/{id}/assets/**")
    public ResponseEntity<?> proxyFlinkAssets(
                                              HttpServletRequest request, @PathVariable("type") String type,
                                              @PathVariable("id") Long id) throws Exception {
        return proxy(type, request, id);
    }

    @GetMapping("{type}/{id}/**")
    @RequiresPermissions("app:view")
    public ResponseEntity<?> proxyFlink(
                                        HttpServletRequest request, @PathVariable("type") String type,
                                        @PathVariable("id") Long id) throws Exception {
        return proxy(type, request, id);
    }

    private ResponseEntity<?> proxy(String type, HttpServletRequest request, Long id) throws Exception {
        switch (type) {
            case "flink":
                return proxyService.proxyFlink(request, id);
            case "cluster":
                return proxyService.proxyCluster(request, id);
            case "history":
                return proxyService.proxyHistory(request, id);
            case "yarn":
                return proxyService.proxyYarn(request, id);
            default:
                return ResponseEntity.notFound().build();
        }
    }
}
