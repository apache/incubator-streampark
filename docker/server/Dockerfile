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
FROM maven:3.8.4-jdk-8 as deps-stage
COPY . /
WORKDIR /
RUN tar zxvf streamx-console/streamx-console-service/target/streamx-console-service-*.tar.gz \
&& mv streamx-console-service-* streamx


FROM maven:3.8.4-jdk-8
WORKDIR /streamx
COPY --from=deps-stage /streamx /streamx

ENV NODE_VERSION 16.1.0
ENV NPM_VERSION 7.11.2

ENV FLINK_VERSION 1.13.6
ENV SCALA_VERSION scala-2.11


RUN wget "https://nodejs.org/dist/v$NODE_VERSION/node-v$NODE_VERSION-linux-x64.tar.gz" \
    && tar zxvf "node-v$NODE_VERSION-linux-x64.tar.gz" -C /usr/local --strip-components=1 \
    && rm "node-v$NODE_VERSION-linux-x64.tar.gz"
    && wget "https://dlcdn.apache.org/flink/$FLINK_VERSION/$FLINK_VERSION-bin-$SCALA_VERSION.tgz" \
    && tar zxvf "flink-$FLINK_VERSION-bin-$SCALA_VERSION.tgz" -C /usr/local --strip-components=1 \
    && rm "flink-$FLINK_VERSION-bin-$SCALA_VERSION.tgz"
EXPOSE 10000
ENTRYPOINT bash bin/startup.sh
