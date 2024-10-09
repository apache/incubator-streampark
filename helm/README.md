<!--
  ~ Licensed to the Apache Software Foundation (ASF) under one or more
  ~ contributor license agreements.  See the NOTICE file distributed with
  ~ this work for additional information regarding copyright ownership.
  ~ The ASF licenses this file to You under the Apache License, Version 2.0
  ~ (the "License"); you may not use this file except in compliance with
  ~ the License.  You may obtain a copy of the License at
  ~
  ~    http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  ~
  -->
# Deploy StreamPark on k8s

### 1.Prepare the StreamPark Image with Flink
#### 1. Create a Dockerfile to include Flink within the StreamPark image.
```dockerfile
FROM apache/streampark:2.1.5
# add your-flink-version.tgz /opt/
# such as
ADD dist/flink-1.19.1-bin-scala_2.12.tgz /opt/
ADD dist/flink-1.17.2-bin-scala_2.12.tgz /opt/

# you can include mysql connector into streampark lib if needed
COPY lib/mysql-connector-j-8.4.0.jar /streampark/lib/
```

#### 2. Build the Docker image
```shell
docker build -t yourdomain/yournamespace/streampark:2.1.5-with-flink -f yourDockerfile .
```

### 2. Deploy StreamPark to Kubernetes
#### 1. Create a namespace for StreamPark
```shell
kubectl create ns streampark
```
#### 2.  Create a secret for the KubeConfig
```shell
kubectl create secret generic kube-config --from-file=/root/.kube/config -n streampark
```
### 3. Deploying StreamPark to Kubernetes
Choose one of the following options:
#### Option 1: using a template
```shell
helm template streampark/ -n default -f streampark/values.yaml --output-dir ./result
kubectl apply -f result/streampark/templates -n streampark
```
#### Option 2: direct deployment
```shell
helm install streampark helm/streampark -n streampark
```

### 3. open WebUI

http://${host}:10000

#### [more detail](streampark/templates/NOTES.txt)
