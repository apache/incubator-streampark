# StreamX
let't flink|spark easy


> 一个神奇的框架,让Flink开发更简单

## 什么是Streamx?
&nbsp;&nbsp;&nbsp;&nbsp;大数据技术如今发展的如火如荼,已经呈现欣欣向荣百花齐放的景象,实时处理流域 `Apache Spark` 和 `Apache Flink` 更是一个伟大的进步,尤其是`Apache Flink`被普遍认为是下一代大数据流计算引擎,我们在使用 `Flink` 时发现从编程模型, 启动配置到运维管理都有很多可以抽象共用的地方,
我们将一些好的经验固化下来并结合业内的最佳实践, 通过不断努力终于诞生了今天的框架 —— `StreamX`, 项目的初衷是 —— 让 `Flink` 开发更简单, 使用`StreamX`开发,可以极大降低学习成本和开发门槛,
让你只用关心最核心的业务,`StreamX` 规范了项目的配置,鼓励函数式编程,定义了最佳的编程方式,提供了一系列开箱即用的`Connector`,标准化了配置、开发、测试、部署、监控、运维的整个过程, 提供`scala`和`java`两套api,
其最终目的是打造一个一站式大数据平台,流批一体,湖仓一体的解决方案
<video src="http://assets.streamxhub.com/streamx.mp4" controls="controls" autoplay="autoplay" width="100%" height="100%"></video>

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

## 组成部分

`Streamx`有三部分组成,`streamx-core`,`streamx-pump` 和 `streamx-console`

### streamx-core

`streamx-core` 定位是一个开发时框架,关注编码开发,规范了配置文件,按照约定优于配置的方式进行开发,提供了一个开发时 `RunTime Content`和一系列开箱即用的`Connector`,扩展了`DataStream`相关的方法,融合了`DataStream`和`Flink sql` api,简化繁琐的操作,聚焦业务本身,提高开发效率和开发体验

### streamx-pump

`pump` 是抽水机,水泵的意思,`streamx-pump`的定位是一个数据抽取的组件,类似于`flinkx`,基于`streamx-core`中提供的各种`connector`开发,目的是打造一个方便快捷,开箱即用的大数据实时数据抽取和迁移组件,并且集成到`streamx-console`中,解决实时数据源获取问题,目前在规划中

### streamx-console

`streamx-console` 是一个综合实时数据平台,低代码(`Low Code`)平台,可以较好的管理`Flink`任务,集成了项目编译、发布、参数配置、启动、`savepoint`,火焰图(`flame graph`),`Flink SQL`,监控等诸多功能于一体,大大简化了`Flink`任务的日常操作和维护,融合了诸多最佳实践。旧时王谢堂前燕,飞入寻常百姓家,让大公司有能力研发使用的项目,现在人人可以使用,其最终目标是打造成一个实时数仓,流批一体的一站式大数据解决方案
,该项目提供了租户和权限管理,代码风格充分遵守阿里的开发规范,结构也尽可能的清晰规范,可以作为大数据平台的开发基础,很方便地进行二次开发,该平台使用到但不仅限以下技术
* [Apache Flink](http://flink.apache.org)
* [Apache YARN](http://hadoop.apache.org)
* [Spring Boot](https://spring.io/projects/spring-boot/)
* [Mybatis](http://www.mybatis.org)
* [Mybatis-Plus](http://mp.baomidou.com)
* [Flame Graph](http://www.brendangregg.com/FlameGraphs)
* [JVM-Profiler](https://github.com/uber-common/jvm-profiler)
* [Vue](https://cn.vuejs.org/)
* [VuePress](https://vuepress.vuejs.org/)
* [Ant Design of Vue](https://antdv.com/)
* [ANTD PRO VUE](https://pro.antdv)
* [xterm.js](https://xtermjs.org/)
* [Monaco Editor](https://microsoft.github.io/monaco-editor/)
* ...

感谢以上优秀的开源项目和很多未提到的优秀开源项目,给予最大的respect,特别感谢[Apache Zeppelin](http://zeppelin.apache.org)在开发`NoteBook`模块时从中借鉴了大量思想,
特别感谢[IntelliJ IDEA](https://www.jetbrains.com/idea/), 感谢[fire-spark](https://github.com/GuoNingNing/fire-spark)项目,早期给予的灵感和帮助,
感谢我老婆在项目开发时给予的支持,悉心照顾我的生活和日常,给予我足够的时间开发这个项目

## Features
* 开发脚手架
* 从任务开发阶段到部署管理全链路支持  
* 一系列开箱即用的connectors
* 支持项目编译功能(maven 编译)
* 在线参数配置
* 支持`Applicaion` 模式, `Yarn-Per-Job`模式启动
* 快捷的日常操作(任务`启动`、`停止`、`savepoint`,从`savepoint`恢复)
* 支持火焰图
* 支持`notebook`(在线任务开发)
* 项目配置和依赖版本化管理
* 支持任务备份、回滚(配置回滚)
* 在线管理依赖(maven pom)和自定义jar
* 自定义udf、连接器等支持 
* Flink SQL WebIDE
* 支持catalog、hive  
* ***

## 为什么不是...?

### Apache Zeppelin

[Apache Zeppelin](http://zeppelin.apache.org)是一个非常优秀的开源项目,对`Flink`做了很好的支持,`Zeppelin`创新型的`notebook`功能,让开发者非常方便的`On-line`编程,快捷的提交任务,语言层面同时支持`java`,`scala`,`python`,国内阿里的章剑峰大佬也在积极推动该项目,向剑峰大佬致以崇高的敬意,
但该项目目前貌似没有解决项目的管理和运维方面的痛点,针对比较复杂的项目和大量的作业管理就有些力不从心了,一般来讲不论是`DataStream`作业还是`Flink SQL`作业,大概都会经历作业的`开发阶段`,`测试阶段`,`打包阶段`,`上传服务器阶段`,`启动任务阶段`等这些步骤,这是一个链路很长的步骤,且整个过程耗时比较长,体验不好,
即使修改了一个符号,项目改完上线都得走上面的流程,我们期望这些步骤能够动动鼠标一键式解决,还希望至少能有一个任务列表的功能,能够方便的管理任务,可以清楚的看到哪些任务正在运行,哪些停止了,任务的资源消耗情况,可以在任务列表页面一键`启动`或`停止`任务,并且自动管理`savePoint`,这些问题也是开发者实际开发中会遇到了问题,
`streamx-console`很好的解决了这些痛点,定位是一个一站式实时数据平台,并且开发了更多令人激动的功能(诸如`Flink SQL WebIDE`,`依赖隔离`,`任务回滚`,`火焰图`等)

### FlinkX

[FlinkX](http://github.com/DTStack/flinkx) 是基于flink的分布式数据同步工具,实现了多种异构数据源之间高效的数据迁移,定位比较明确,专门用来做数据抽取和迁移,可以作为一个服务组件来使用,`StreamX`关注开发阶段和任务后期的管理,定位有所不同,`streamx-pump`模块也在规则中,
致力于解决数据源抽取和迁移,最终会集成到`streamx-console`中


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

### 1. 代码开发

现在让我们快速上手一个`Streamx`应用

> 确保flink版本是 1.12.0 +

```scala

package com.your.flink.streamx

import com.streamxhub.streamx.flink.core.scala.sink.KafkaSink
import com.streamxhub.streamx.flink.core.scala.source.KafkaSource
import com.streamxhub.streamx.flink.core.scala.{FlinkStreaming, StreamingContext}
import org.apache.flink.api.scala._

object HelloStreamXApp extends FlinkStreaming {

  override def handle(): Unit = {
    //1) source
    val source = KafkaSource().getDataStream[String](topic = "hello")
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
  checkpoints:
    enable: true
    interval: 30000
    mode: EXACTLY_ONCE
    timeout: 300000
    unaligned: true
  watermark:
    interval: 10000
  # 状态后端
  state:
    backend: # see https://ci.apache.org/projects/flink/flink-docs-release-1.12/ops/state/state_backends.html
      value: filesystem # 保存类型('jobmanager', 'filesystem', 'rocksdb')
      memory: 5242880 # 针对jobmanager有效,最大内存
      async: false    # 针对(jobmanager,filesystem)有效,是否开启异步
      incremental: true #针对rocksdb有效,是否开启增量
      #rocksdb 的配置参考 https://ci.apache.org/projects/flink/flink-docs-release-1.12/deployment/config.html#rocksdb-state-backend
      #rocksdb配置key的前缀去掉:state.backend
      #rocksdb.block.blocksize:
    checkpoints.dir: file:///tmp/chkdir
    savepoints.dir: file:///tmp/chkdir
  # 重启策略
  restart-strategy:
    value: fixed-delay  #重启策略[(fixed-delay|failure-rate|none)共3个可配置的策略]
    fixed-delay:
      attempts: 3
      delay: 5000
    failure-rate:
      max-failures-per-interval:
      failure-rate-interval:
      delay:
  # table
  table:
    planner: blink # (blink|old|any)
    mode: streaming #(batch|streaming)

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

`Streamx`已正式开源,会进入高速发展模式,更多开发相关信息请访问[官网](http://www.streamxhub.com/#/)


## 安装streamx-console

> streamx-console 是一个综合实时数据平台,低代码(Low Code),Flink Sql平台,可以较好的管理Flink任务,集成了项目编译、发布、参数配置、启动、savepoint,火焰图(flame graph),Flink SQL,监控等诸多功能于一体,大大简化了Flink任务的日常操作和维护,融合了诸多最佳实践。其最终目标是打造成一个实时数仓,流批一体的一站式大数据解决方案

## 如何安装

streamx-console 提供了开箱即用的安装包,安装之前对环境有些要求,具体要求如下

### 环境

* 操作系统	Linux(不支持Window系统)
* JAVA	    1.8+
* MySQL	    5.6+
* Hadoop 	2+ (HDFS,YARN等必须安装,并且配置好相关环境变量)
* Flink	    1.12.0+ (版本必须是1.12.0或以上版本,并且配置好Flink相关环境变量)
* Python    2+ (非必须,火焰图功能会用到Python)
* Perl  o

### 安装

在安装前一定要确保当前部署的机器满足上面环境相关的要求,当前安装的机器必须要有Hadoop环境,安装并配置好了[Flink 1.12.0+](https://www.apache.org/dyn/closer.lua/flink/flink-1.12.0/flink-1.12.0-bin-scala_2.11.tgz),如果准备工作都已就绪,就可以按照了,点击[这里]()下载streamx-console安装包,解包后安装目录如下

```textmate 
.
streamx-console-service-1.0.0
├── bin
│    ├── flame-graph
│    ├──   └── *.py                             //火焰图相关功能脚本(内部使用,用户无需关注)
│    ├── startup.sh                             //启动脚本  
│    ├── setclasspath.sh                        //java环境变量相关的脚本(内部使用,用户无需关注)
│    ├── shutdown.sh                            //停止脚本
│    ├── yaml.sh                                //内部使用解析yaml参数的脚本(内部使用,用户无需关注)
├── conf                           
│    ├── application.yaml                       //项目的配置文件(注意不要改动名称)
│    ├── application-prod.yml                   //项目的配置文件(开发者部署需要改动的文件,注意不要改动名称)
│    ├── flink-application.template             //flink配置模板(内部使用,用户无需关注)
│    ├── logback-spring.xml                     //logback
│    ├── streamx-console.sql                    //工程初始化脚本
│    └── ...
├── lib
│    └── *.jar                                  //项目的jar包
├── plugins   
│    ├── streamx-jvm-profiler-1.0.0.jar         //jvm-profiler,火焰图相关功能(内部使用,用户无需关注)
│    └── streamx-flink-sqlcli-1.0.0.jar         //Flink SQl提交相关功能(内部使用,用户无需关注)
├── logs                                        //程序log目录
└── temp                                        //内部使用到的零时路径,不要删除
```

#### 1.初始化工程SQL

streamx-console要求的数据库是MySQL,版本5.6+以上,如准备就绪则进行下面的操作:

* 创建数据库:`streamx`
* 执行初始化sql (解包后的`conf/streamx-console.sql`)

#### 2.修改相关的数据库信息

工程SQL初始化完毕,则修改`conf/application-prod.yml`,找到datasource这一项,找到mysql的配置,修改成对应的信息即可,如下

```yaml
  datasource:
    dynamic:
      # 是否开启 SQL日志输出，生产环境建议关闭，有性能损耗
      p6spy: true
      hikari:
        connection-timeout: 30000
        max-lifetime: 1800000
        max-pool-size: 15
        min-idle: 5
        connection-test-query: select 1
        pool-name: HikariCP-DS-POOL
      # 配置默认数据源
      primary: primary
      datasource:
        # 数据源-1，名称为 primary
        primary:
          username: $user
          password: $password
          driver-class-name: com.mysql.cj.jdbc.Driver
          url: jdbc:mysql://$host:$port/streamx?useUnicode=true&characterEncoding=UTF-8&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=GMT%2B8
```

#### 3.启动streamx-console

进入到`bin`下直接执行start.sh即可启动项目,默认端口是 10000,如果没啥意外则会启动成功

```bash
cd streamx-console-service-1.0.0/bin
bash start.sh
```
相关的日志会输出到 streamx-console-service-1.0.0/logs/streamx.out 里

打开浏览器 输入 http://$deploy_host:10000/index.html 即可登录,登录界面如下

<img src="http://assets.streamxhub.com/1617875805692.jpg"/>

默认密码: <strong> admin / streamx </strong>

## 如何使用

streamx-console定位是流批一体的大数据平台,一站式解决方案,使用起来非常简单,没有复杂的概念和繁琐的操作,标准的Flink程序(安装Flink官方要去的结构和规范)和用`streamx`开发的项目都做了很好的支持,下面我们使用`streamx-quickstart`来快速开启streamx-console之旅

`streamx-quickstart`是StreamX 开发Flink的上手示例程序,具体请查阅[这里](https://github.com/streamxhub/streamx-quickstart.git)

* Github: [https://github.com/streamxhub/streamx-quickstart.git](https://github.com/streamxhub/streamx-quickstart.git)
* Gitee: [https://gitee.com/benjobs/streamx-quickstart.git](https://gitee.com/benjobs/streamx-quickstart.git)

<video src="http://assets.streamxhub.com/20210408008.mp4" controls="controls" width="100%" height="100%"></video>

更多使用教程和文档后续会更新...
