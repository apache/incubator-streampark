/*
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

export function baseUrl() {
  let url = ''
  switch (process.env.NODE_ENV) {
    //混合打包(production,不用配置,maven编译项目阶段-Denv=prod自动将环境参数透传到这里)
    case 'production':
      url = (arguments[0] || null) ? (location.protocol + '//' + location.host) : '/'
      break
    //开发测试阶段采用前后端分离,这里配置后端的请求URI
    case 'development':
      url = 'http://localhost:10000'
      break
  }
  return url
}
