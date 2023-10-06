
### 1. linux install pushgateway

#### 1.1. Download pushgateway

```shell
wget https://github.com/prometheus/pushgateway/releases/download/v1.6.2/pushgateway-1.6.2.linux-amd64.tar.gz

tar -zxvf pushgateway-1.6.2.linux-amd64.tar.gz

mv pushgateway-1.6.2.linux-amd64 pushgateway
```

#### 1.2. Start pushgateway

```shell
cd pushgateway

nohup ./pushgateway --web.listen-address :9091 > ./pushgateway.log 2>&1 &
```

### 2. linux install prometheus

#### 2.1. Download prometheus

```shell
wget https://github.com/prometheus/prometheus/releases/download/v2.47.1/prometheus-2.47.1.linux-amd64.tar.gz

tar -zxvf prometheus-2.47.1.linux-amd64.tar.gz

mv prometheus-2.47.1.linux-amd64 prometheus
```

#### 2.2. Modified prometheus.yml, added

```text
  - job_name: "pushgateway"
    static_configs:
      - targets: ["ip:9091"]
        labels:
          instance: pushgateway
```

#### 2.3. Start prometheus

```shell
cd prometheus

nohup ./prometheus --config.file=prometheus.yml > ./prometheus.log 2>&1 &
```

### 3. Modify the streampark configuration

```text
org.apache.streampark.common.conf.CommonConfig.STREAMPARK_PROMETHEUS_ENABLE
streampark.prometheus.enable: true

org.apache.streampark.common.conf.CommonConfig.STREAMPARK_PROMGATEWAY_HOST_URL
streampark.promgateway.hostUrl: http://ip:9091
```
Current only yarn-application is supported. Other modes have not been tested

#### 4. Stop

#### 4.1. Stop prometheus

```shell
pgrep prometheus

kill {pid}
```

#### 4.2. Stop pushgateway

```shell
pgrep pushgateway

kill {pid}
```

#### 5. Reference document

```text
https://nightlies.apache.org/flink/flink-docs-release-1.16/zh/docs/deployment/metric_reporters/#prometheuspushgateway

https://prometheus.io/docs/prometheus/latest/getting_started/
```



