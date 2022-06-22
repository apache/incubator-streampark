# Streamx on Docker and Kubernetes Quick Start

## Docker Quick Start

### Prepare

    Docker 1.13.1+
    Docker Compose 1.28.0+

### Using docker-compose to Start Server
After installed docker-compose, it is recommended to modify some configurations for better experience. We highly recommended modify docker-compose's memory up to 4 GB.

    Mac：Click Docker Desktop -> Preferences -> Resources -> Memory modified it
    Windows Docker Desktop：
    Hyper-V mode: Click Docker Desktop -> Settings -> Resources -> Memory modified it
    WSL 2 mode: see WSL 2 utility VM for more detail.


After complete the configuration, we can get the docker-compose.yaml file from download page form its source package, and make sure you get the right version. After download the package, you can run the commands as below.

1.Environment build via mvn
```
mvn clean install -Dscala.version=2.12.5 -Dscala.binary.version=2.12 -DskipTests
```

2.Execution Docker compose build command
```
docker-compose up -d
```

3.Set up flink home on the streamx web ui
```
/streamx/flink/
```

4.Setting up remote session clusters

5.Complete the above steps and execute the flink task submit

## QuickStart in Kubernetes

### Prerequisites

    Helm version 3.1.0+
    Kubernetes version 1.12+
### Install Streamx




