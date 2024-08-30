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

# Install basic tools
RUN apt update && apt install -y wget curl vim net-tools iputils-ping tini openjdk-11-jdk maven && apt clean

ENV JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
ENV MAVEN_HOME=/usr/share/maven
ENV PATH=$JAVA_HOME/bin:$PATH
ENV PATH=$MAVEN_HOME/bin:$PATH

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

RUN curl -LO "https://dl.k8s.io/release/v1.23.0/bin/linux/amd64/kubectl" && \
    chmod +x kubectl && \
    mv kubectl /usr/local/bin/kubectl

# Install StreamPark
COPY dist/apache-streampark_*-bin.tar.gz /
RUN tar -zxvf apache-streampark_*-bin.tar.gz \
    && mv apache-streampark_*-bin streampark \
    && rm -f apache-streampark_*-bin.tar.gz


ENTRYPOINT ["/usr/bin/tini", "--", "/streampark/bin/streampark.sh", "start_docker"]
