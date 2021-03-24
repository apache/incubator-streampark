# StreamX
let't flink|spark easy

## 什么是Streamx?

大数据实时处理流域 `Apache Spark` 和 后起之秀 `Apache Flink` 是一个伟大的进步,从`Hadoop MapReduce`的刀耕火种时代迈入了高铁快车时代,我们在使用 Flink & Spark 时发现从编程模型,
启动配置到管理运维都有很多可以抽象共用的地方, 我们将一些好的经验固化下来并结合业内的最佳实践,通过不断努力终于诞生了今天的框架 —— Streamx, 项目的初衷是:让 Flink & Spark开发更简单,
使用Streamx可以极大降低学习成本和开发门槛, 让你只用关心最核心的业务,Streamx 规范了项目的配置,定义了最佳的编程方式,提供了一系列开箱即用的Connector,标准化了配置、开发、测试、部署、监控、运维的整个过程,
同时提供`scala`和`java`两套api,其最终目的是打造一个一站式大数据平台,流批一体的解决方案.
Streamx有两部分组成 —— `Streamx-core` 和 `Streamx-console`

#### 消费kafka示例

```yaml
kafka.source:
    bootstrap.servers: kfk1:9092,kfk2:9092,kfk3:9092
    topic: test_user
    group.id: user_01
    auto.offset.reset: earliest
    enable.auto.commit: true
```

```scala
KafkaSource().getDataStream[String]().print()
```

## Streamx-core

Streamx-core (Streamx-flink-core|Streamx-spark-core) 是一个开发时的框架,借鉴了SpringBoot的思想,规范了配置文件的格式,按照约定优于配置。为开发者提供了一个开发时 RunTime Content,提供了一个开箱即用的Source和Sink,每一个API都经过了仔细的打磨，并扩展了相关的方法（仅scala）大大简化了flink的开发，提高了开发效率和开发体验

## Streamx-console

Streamx-console 是一个独立的平台，它补充了Streamx-core。较好地管理了flink任务，集成了项目编译、发布、参数配置、启动、savepoint、监控和维护等功能，并且集成了火焰图(flame graph),大大简化了flink任务的操作和维护。该平台本身采用SpringBoot Vue Mybatis开发,提供了简单的租户和权限管理,代码风格充分遵守阿里的开发规范,结构也尽可能的清晰规范,可以作为大数据平台的开发基础，很方便地进行二次开发

![console dashboard](http://assets.streamxhub.com/console-dashboard.jpg)

![job flameGraph](http://assets.streamxhub.com/job-flameGraph.png)

## 如何编译

我们要做的第一件事就是将项目clone到本地,执行编译,在编译前请确保以下事项

* 确保本机安装的JDK`1.8`及以上的版本
* 确保本机已经安装了maven

如果准备就绪,就可以clone项目并且执行编译了

```bash
git clone https://github.com/Streamxhub/Streamx.git
cd Streamx
mvn clean install -DskipTests
```

顺利的话就会看到编译成功.

```log
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for Streamx 1.0.0:
[INFO]
[INFO] Streamx ............................................ SUCCESS [  1.882 s]
[INFO] Streamx : Common ................................... SUCCESS [ 15.700 s]
[INFO] Streamx : Flink Parent ............................. SUCCESS [  0.032 s]
[INFO] Streamx : Flink Common ............................. SUCCESS [  8.243 s]
[INFO] Streamx : Flink Core ............................... SUCCESS [ 17.332 s]
[INFO] Streamx : Flink Test ............................... SUCCESS [ 42.742 s]
[INFO] Streamx : Spark Parent ............................. SUCCESS [  0.018 s]
[INFO] Streamx : Spark Core ............................... SUCCESS [ 12.028 s]
[INFO] Streamx : Spark Test ............................... SUCCESS [  5.828 s]
[INFO] Streamx : Spark Cli ................................ SUCCESS [  0.016 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  01:43 min
[INFO] Finished at: 2021-03-15T17:02:22+08:00
[INFO] ------------------------------------------------------------------------
```

编译完成后,项目会安装到本地的maven仓库里,在使用时需要引入pom

```xml
<dependency>
    <groupId>com.streamxhub.streamx</groupId>
    <artifactId>streamx-flink-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 快速上手

现在让我们快速上手一个`Streamx`应用

> 确保flink版本是 1.12.0 +

```scala

package com.your.flink.streamx

import com.streamxhub.streamx.flink.core.scala.sink.KafkaSink
import com.streamxhub.streamx.flink.core.scala.source.KafkaSource
import com.streamxhub.streamx.flink.core.scala.{FlinkStreaming, StreamingContext}
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

### 2. 定义配置文件application.yml

在配置文件中定义一系列启动信息以及Connector相关信息,具体格式如下：

```yaml
flink:
  deployment: #注意这里的参数一定能要flink启动支持的参数(因为在启动参数解析时使用了严格模式,一个不识别会停止解析),详情和查看flink官网,否则会造成整个参数解析失败,最明显的问题的找不到jar文件
    option:
      target: yarn-per-job              # --target <arg> (local|remote|yarn-per-job|yarn-session|run-application)
      detached:                         # -d   (If present, runs the job in detached mode)
      shutdownOnAttachedExit:           # -sae (If the job is submitted in attached mode, perform a best-effort cluster shutdown when the CLI is terminated abruptly, e.g., in response to a user interrupt, such as typing Ctrl + C.)
      zookeeperNamespace:               # -z Namespace to create the Zookeeper sub-paths  for high availability mode
      jobmanager:                       #  -m Address of the JobManager to which to connect. Use this flag to connect to a different JobManager than the one specified in the configuration. Attention: This option is respected only if the  high-availability configuration is NONE
    property:                           # see: https://ci.apache.org/projects/flink/flink-docs-release-1.12/deployment/config.html
      $internal.application.main: com.your.flink.streamx.HelloStreamXApp # main class
      yarn.application.name: FlinkHelloWorldApp
      yarn.application.node-label: StreamX
      taskmanager.numberOfTaskSlots: 1
      parallelism.default: 2
      jobmanager.memory:
        flink.size:
        heap.size:
        jvm-metaspace.size:
        jvm-overhead.max:
        off-heap.size:
        process.size:
      taskmanager.memory:
        flink.size:
        framework.heap.size:
        framework.off-heap.size:
        managed.size:
        process.size:
        task.heap.size:
        task.off-heap.size:
        jvm-metaspace.size:
        jvm-overhead.max:
        jvm-overhead.min:
        managed.fraction: 0.4
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
  topic: topic1,topic2,topic3
  group.id: hello
  auto.offset.reset: earliest
  #enable.auto.commit: true
  #start.from:
    #timestamp: 1591286400000 #指定timestamp,针对所有的topic生效
    #offset: # 给每个topic的partition指定offset
      #topic: topic1,topic2,topic3
      #topic1: 0:182,1:183,2:182 #分区0从182开始消费,分区1从183...
      #topic2: 0:182,1:183,2:182
      #topic3: 0:192,1:196,2:196


kafka.sink:
  bootstrap.servers: kafka1:9092,kafka2:9092,kafka3:9092
  topic: kfk_sink
  transaction.timeout.ms: 1000
  semantic: AT_LEAST_ONCE # EXACTLY_ONCE|AT_LEAST_ONCE|NONE
  batch.size: 1

```

### 3. 运行程序

启动main方法,并且跟上参数" --flink.conf $path/application.yml"

`Streamx`已正式开源,会进入高速发展模式,更多开发相关信息请访问[官网](http://www.streamxhub.com/#/) 或者扫下面的二维码加入用户讨论群

<img width="250px" height="250px" src="http://assets.streamxhub.com/streamx_wechat.png"/>
