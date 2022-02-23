# Any Error will stop build.
set -e
WORK_SPACE="/opt/streamx/bin"
if [ ! -d $WORK_SPACE ]; then
 tar xf /tmp/streamx.gz -C /opt  \
 && cp -R /opt/streamx-console-service-${STREAMX_VERSION}/* /opt/streamx     \
 && mkdir /opt/jdk  \
 && tar xf /tmp/jdk-8.tar.gz -C /opt/jdk --strip-components 1  \
 && sed -i 's/localhost:3306/streamx-mysql-docker:3306/g' /opt/streamx/conf/application.yml \
 && rm -rf /tmp/* \
 && rm -rf /opt/streamx-console-service-${STREAMX_VERSION} \
 && echo "Initialize the project successfully."
 /bin/bash /opt/streamx/bin/startup.sh
 tail -f /opt/streamx/logs/*
else
    # Prevent mysql not ready.
    sleep 5
    echo "Starting streamx."
    /bin/bash /opt/streamx/bin/startup.sh
    tail -f /opt/streamx/logs/*
fi
