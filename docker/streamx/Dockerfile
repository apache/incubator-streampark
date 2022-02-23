FROM centos:centos7

ARG streamx_version=???

MAINTAINER streamx@whojohn

COPY streamx.gz jdk-8.tar.gz /tmp/
COPY start.sh /opt/

#RUN tar xf /tmp/streamx-console-service-${streamx_version}-bin.tar.gz -C /opt  \
# && ln -s /opt/streamx-console-service-${streamx_version} /opt/streamx     \
# && mkdir /opt/jdk  \
# && tar xf /tmp/jdk-8.tar.gz -C /opt/jdk --strip-components 1  \
# && mv /tmp/start.sh /opt/streamx/ \
# && rm -rf /tmp/* \
# && sed -i 's/localhost:3306/streamx-mysql-docker:3306/g' /opt/streamx/conf/application.yml

EXPOSE 10000

ENV JAVA_HOME=/opt/jdk \
    STREAMX_HOME=/opt/streamx \
    STREAMX_VERSION=${streamx_version}

ENV PATH=${PATH}:${JAVA_HOME}/bin

ENTRYPOINT [ "/bin/bash", "/opt/start.sh" ]
