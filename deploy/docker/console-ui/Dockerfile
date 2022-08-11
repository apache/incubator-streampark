###############################################################################
#  Copyright (c) 2019 The StreamX Project
#  Licensed to the Apache Software Foundation (ASF) under one or more
#  contributor license agreements.  See the NOTICE file distributed with
#  this work for additional information regarding copyright ownership.
#  The ASF licenses this file to You under the Apache License, Version 2.0
#  (the "License"); you may not use this file except in compliance with
#  the License.  You may obtain a copy of the License at
#
#  https://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
###############################################################################
FROM node:14.16.0-alpine AS build

COPY streamx-console/streamx-console-webapp /streamx-webapp
WORKDIR /streamx-webapp

RUN npm config set registry https://registry.npmmirror.com \
    && npm install \
    && npm run build

FROM nginx:stable-alpine

COPY --from=build /streamx-webapp/dist /streamx-webapp
COPY deploy/docker/console-ui/nginx.conf /etc/nginx/conf.d/default.conf