#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
FROM ubuntu:22.04

USER root

RUN sed -i "s/ports.ubuntu.com\/ubuntu-ports\//mirrors.tuna.tsinghua.edu.cn\/ubuntu-ports\//g" /etc/apt/sources.list
# Install basic tools
RUN apt update && apt install -y wget curl vim net-tools iputils-ping \
    && apt install software-properties-common -y \
    && add-apt-repository ppa:openjdk-r/ppa -y
RUN apt update && apt install -y openjdk-8-jdk
RUN JAVA_PATH=$(ls -l /usr/lib/jvm | grep java-8-openjdk | grep ^d | awk -F ' ' '{print $9}'); \
    if [ -z "${JAVA_PATH}" ];then \
        echo "JAVA_PATH not found: $JAVA_PATH"; \
        exit 2; \
    else \
        ln -s /usr/lib/jvm/$JAVA_PATH/ /usr/lib/jvm/jdk8; \
    fi
ENV JAVA_HOME=/usr/lib/jvm/jdk8


# Install docker
RUN \
    # Add Docker's official GPG key:
    apt update && \
    apt install -y ca-certificates curl gnupg && \
    install -m 0755 -d /etc/apt/keyrings && \
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg && \
    chmod a+r /etc/apt/keyrings/docker.gpg && \
    # Add the repository to Apt sources:
    echo \
        "deb [arch="$(dpkg --print-architecture)" signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
        "$(. /etc/os-release && echo "$VERSION_CODENAME")" stable" | \
        tee /etc/apt/sources.list.d/docker.list > /dev/null && \
    apt update && \
    # Install the Docker packages.
    apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin


# Install Tini
ARG TARGETPLATFORM
ENV TINI_VERSION v0.19.0
RUN echo "TARGETPLATFORM: $TARGETPLATFORM"
RUN \
    if [ "$TARGETPLATFORM" = "linux/amd64" ];then \
        TINI_PLATFORM=amd64; \
        wget --no-check-certificate -O /usr/sbin/tini https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini-$TINI_PLATFORM; \
    elif [ "$TARGETPLATFORM" = "linux/arm64" ];then \
        TINI_PLATFORM=arm64; \
        wget --no-check-certificate -O /usr/sbin/tini https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini-$TINI_PLATFORM; \
    else \
        echo "unknown TARGETPLATFORM: $TARGETPLATFORM"; \
        exit 2;  \
    fi
RUN chmod +x /usr/sbin/tini


# Install StreamPark
COPY dist/apache-streampark*-*-bin.tar.gz /
RUN tar -zxvf apache-streampark*-*-bin.tar.gz \
    && mv apache-streampark*-*-bin streampark \
    && rm -f apache-streampark*-*-bin.tar.gz


ENTRYPOINT ["/usr/sbin/tini", "--", "/streampark/bin/streampark.sh", "start_docker"]
