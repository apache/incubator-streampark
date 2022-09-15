#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

FROM docker.io/bitnami/spark:3.2.0
LABEL description="Docker image with Spark (3.2.0) and Hadoop (3.3.2) and Hive (3.1.3), based on bitnami/spark:3.2.0. \
For more information, please visit https://github.com/s1mplecc/spark-hadoop-docker."

USER root

ENV HADOOP_HOME="/opt/hadoop"
ENV HADOOP_CONF_DIR="$HADOOP_HOME/etc/hadoop"
ENV HADOOP_LOG_DIR="/var/log/hadoop"
ENV HIVE_HOME="/opt/hive"
ENV HIVE_CONF_DIR="$HADOOP_HOME/etc/hive"
ENV HIVE_LOG_DIR="/var/log/hive"
ENV PATH="$HADOOP_HOME/hadoop/sbin:$HADOOP_HOME/bin:$HIVE_HOME/hive/sbin:$HIVE_HOME/bin:$PATH"


WORKDIR /opt

RUN apt-get update && apt-get install -y openssh-server

RUN ssh-keygen -t rsa -f /root/.ssh/id_rsa -P '' && \
    cat /root/.ssh/id_rsa.pub >> /root/.ssh/authorized_keys

RUN curl -OL https://archive.apache.org/dist/hadoop/common/hadoop-3.3.1/hadoop-3.3.1.tar.gz
#ADD hadoop-3.3.2.tar.gz ./
RUN tar -xzvf hadoop-3.3.1.tar.gz
RUN mv hadoop-3.3.1 hadoop && \
	rm -rf hadoop-3.3.1.tar.gz && \
	mkdir /var/log/hadoop

RUN mkdir -p /root/hdfs/namenode && \
    mkdir -p /root/hdfs/datanode

RUN curl -OL https://archive.apache.org/dist/hive/hive-3.1.3/apache-hive-3.1.3-bin.tar.gz
#ADD apache-hive-3.1.3-bin.tar.gz ./
RUN tar -xzvf apache-hive-3.1.3-bin.tar.gz
RUN mv apache-hive-3.1.3-bin hive && \
	rm -rf apache-hive-3.1.3-bin.tar.gz && \
	mkdir /var/log/hive

COPY config/* /tmp/

RUN mv /tmp/ssh_config /root/.ssh/config && chmod 600 /root/.ssh/config &&\
    mv /tmp/hadoop-env.sh $HADOOP_CONF_DIR/hadoop-env.sh && \
    mv /tmp/hdfs-site.xml $HADOOP_CONF_DIR/hdfs-site.xml && \
    mv /tmp/core-site.xml $HADOOP_CONF_DIR/core-site.xml && \
    mv /tmp/mapred-site.xml $HADOOP_CONF_DIR/mapred-site.xml && \
    mv /tmp/yarn-site.xml $HADOOP_CONF_DIR/yarn-site.xml && \
    mv /tmp/workers $HADOOP_CONF_DIR/workers

COPY start-hadoop.sh /opt/start-hadoop.sh

RUN chmod +x /opt/start-hadoop.sh && \
    chmod +x $HADOOP_HOME/sbin/start-dfs.sh && \
    chmod +x $HADOOP_HOME/sbin/start-yarn.sh

RUN hdfs namenode -format
RUN sed -i "1 a /etc/init.d/ssh start > /dev/null &" /opt/bitnami/scripts/spark/entrypoint.sh

ENTRYPOINT [ "/opt/bitnami/scripts/spark/entrypoint.sh" ]
CMD [ "/opt/bitnami/scripts/spark/run.sh" ]
