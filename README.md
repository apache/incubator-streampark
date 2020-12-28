# StreamX
let't flink|spark easy

The flink & spark development scaffolding, encapsulates a series of out-of-the-box source and sink, and standardizes flink & spark development,testing,deployment and monitoring

StreamX consists of two parts, streamx-core and streamx-console,
Streamx-core (Streamx-flink-core|Streamx-spark-core) is a framework for development. Drawing on the idea of springBoot, the convention is better than the configuration. It provides developers with a list of sources and sinks out of the box, and expands related methods (only scala is effective), which greatly simplifies The development of flink greatly improves development efficiency and development experience

Streamx-console is an independent platform that complements streamx-core. It better manages flink tasks, integrates project compilation, release, startup, savepoint, monitoring, operation and maintenance, etc., which greatly simplifies the operation and maintenance of flink tasks. A development base of the flink platform, it is easy to do secondary development based on it

![console dashboard](https://raw.githubusercontent.com/wolfboys/mycdn/master/img/console-dashboard.jpg)

![job flameGraph](https://raw.githubusercontent.com/wolfboys/mycdn/master/img/job-flameGraph.png)


## How to Build

1. Make sure JDK 8+ and maven is installed on your machine.
2. Run: `mvn clean install -DskipTests`

```shell
git clone https://github.com/streamxhub/streamx.git
cd streamx
mvn clean install -DskipTests
```

After the build is completed, the project will be installed in the local maven warehouse and added to the pom.xml file of your own project when using it

```xml
<dependency>
    <groupId>com.streamxhub</groupId>
    <artifactId>streamx-flink-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick Start

### 1. Flink Example with StreamX

1. Make sure flink 1.11.1+

```scala

package com.your.flink.streamx

import com.streamxhub.flink.core.scala.sink.KafkaSink
import com.streamxhub.flink.core.scala.source.KafkaSource
import com.streamxhub.flink.core.scala.{FlinkStreaming, StreamingContext}
import org.apache.flink.api.scala._

object HelloStreamXApp extends FlinkStreaming {

  override def handle(context: StreamingContext): Unit = {
    //1) source
    val source = KafkaSource(context).getDataStream[String](topic = "hello")
      .uid("kfk_source")
      .name("kfk_source")
      .map(_.value)

    //2) println
    source.print()

    //3) sink
    KafkaSink(context).sink(source,topic = "kfk_sink")

  }

}

```

### 2. Define application.yml
Define a series of startup information and source and sink information in the configuration file application.yml. The specific format is as follows:
```yaml

flink:
  ## Start related resource parameters
  deployment: 
    dynamic: ##dynamic properties... -yD key=value
    option: ## Here to start the resource related configuration
      yarnname: FlinkSinkApp            # Set a custom name for the application on YARN
      class: com.your.flink.streamx.HelloStreamXApp        # main class
      detached:                         # -d   (If present, runs the job in detached mode)
      jobmanager: yarn-cluster          # -m   (Address of the JobManager (master) to which to connect. Use this flag to connect to a different JobManager than the one specified in the configuration.)
      shutdownOnAttachedExit:           # -sae (If the job is submitted in attached mode, perform a best-effort cluster shutdown when the CLI is terminated abruptly, e.g., in response to a user interrupt, such as typing Ctrl + C.)
      yarnapplicationType:              # -yat (Set a custom application type for the application on YARN)
      yarnjobManagerMemory: 1024M       # -yjm (Memory for JobManager Container with optional unit (default: MB))
      yarnnodeLabel:                    # -ynl (Specify YARN node label for the YARN application)
      yarnqueue:                        # -yqu (Specify YARN queue)
      yarnslots: 1                      # -ys  (Number of slots per TaskManager)
      yarntaskManagerMemory: 4096M      # -ytm
      parallelism: 1
  watermark:
    time.characteristic: EventTime
    interval: 10000
  checkpoints:
    unaligned: true
    enable: true
    interval: 5000
    mode: EXACTLY_ONCE
  table:
    planner: blink # (blink|old|any)
    mode: streaming #(batch|streaming)


# restart-strategy
restart-strategy: failure-rate #(fixed-delay|failure-rate|none共3个可配置的策略)
# Up to 10 mission failures are allowed within 5 minutes, and restart every 5 seconds after each failure. If the failure rate exceeds this failure rate, the program will exit
restart-strategy.failure-rate:
  max-failures-per-interval: 10
  failure-rate-interval: 5min
  delay: 5000
  #failure-rate:
  #  max-failures-per-interval:
  #  failure-rate-interval:
  #  delay:
  #none:


#state.backend
state.backend: rocksdb #保存类型(jobmanager,filesystem,rocksdb)
state.backend.memory: 5242880 #针对jobmanager有效,最大内存
state.backend.async: false # 针对(jobmanager,filesystem)有效,是否开启异步
state.backend.incremental: true #针对rocksdb有效,是否开启增量
#rocksdb config: https://ci.apache.org/projects/flink/flink-docs-release-1.9/ops/config.html#rocksdb-configurable-options
#state.backend.rocksdb.block.blocksize:
#....

# source config....
kafka.source:
  bootstrap.servers: kafka1:9092,kafka2:9092,kafka3:9092
  topic: hello
  group.id: hello
  auto.offset.reset: earliest
  #enable.auto.commit: true
  #start.from:
    #timestamp: 1591286400000 #指定timestamp,针对所有的topic生效
    #offset: # 给每个topic的partition指定offset
      #topic: kafka01,kafka02
      #kafka01: 0:182,1:183,2:182 #分区0从182开始消费,分区1从183...
      #kafka02: 0:182,1:183,2:182
      #hopsonone_park_gz_tjd_specilog: 0:192,1:196,2:196


kafka.sink:
  bootstrap.servers: kafka1:9092,kafka2:9092,kafka3:9092
  topic: kfk_sink
  transaction.timeout.ms: 1000
  semantic: AT_LEAST_ONCE # EXACTLY_ONCE|AT_LEAST_ONCE|NONE
  batch.size: 1

```

### 3. Run Application
Start main and with argument " --flink.conf $path/application.yml"
