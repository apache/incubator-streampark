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
FROM maven:3.8.5-jdk-8-slim as build
ARG SCALA_VERSION=2.12.7
ARG SCALA_BINARY_VERSION=2.12
ARG WEB_BUILD

COPY . /app
WORKDIR /app

RUN mvn clean install -DskipTests -Dscala.version=$SCALA_VERSION -Dscala.binary.version=$SCALA_BINARY_VERSION $WEB_BUILD \
  && tar zxvf streamx-console/streamx-console-service/target/streamx-console-service-*.tar.gz \
  && mv streamx-console-service-* streamx

FROM maven:3.8.5-jdk-8-slim
ARG FLINK_VERSION=1.14.5
ARG SCALA_VERSION=scala_2.12
ARG DOCKER_VERSION=20.10.11

ENV IS_DOCKER=true
WORKDIR /streamx

RUN apt-get update \
    && apt-get -y install iputils-ping vim net-tools bash wget \
    && wget "https://dlcdn.apache.org/flink/flink-$FLINK_VERSION/flink-$FLINK_VERSION-bin-$SCALA_VERSION.tgz" \
    && mkdir ./flink \
    && tar zxvf "flink-$FLINK_VERSION-bin-$SCALA_VERSION.tgz" -C ./flink --strip-components=1 \
    && rm "flink-$FLINK_VERSION-bin-$SCALA_VERSION.tgz"

COPY --from=build /app/streamx/ /streamx
EXPOSE 10000

ENTRYPOINT [ "/bin/sh", "bin/startup.sh" ]