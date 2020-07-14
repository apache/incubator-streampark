# StreamX
let't spark|flink easy
    
### BUILD和使用方法
#### 1.构建方法

```
    $git clone https://github.com/streamxhub/StreamX.git
    $cd StreamX
    $mvn clean install

    maven构建时已默认在pom中跳过了所有的test代码的编译和测试
    构建完成之后会将jar安装到m2的对应路径下，使用时在自己的项目的pom.xml文件里添加
    
    <dependency>
        <groupId>com.streamxhub</groupId>
        <artifactId>streamx-spark-core</artifactId>
    	<version>1.0.0</version>
    </dependency>
```

#### 2.创建配置

```

可使用spark-cli 中的create_conf.sh创建默认配置文件,使用方法:
$bash 中的create_conf.sh >my.properties


生成的标准配置内容
此配置文件可以自定义添加任何以spark开头的配置参数,spark程序本身的依赖参数也在此配置文件配置

######################################################
#                                                    #
#               spark process startup.sh             #
#                   user config                      #
#                                                    #
######################################################
#必须设置,执行class的全包名称
spark.app.main=com.streamxhub.spark.HelloApp

#提供给执行class的命令行参数,多个参数之间用逗号隔开,参数中不能包含空格等空白符
#Ex:param1,param2,..
spark.app.params=--checkpointPath,/tmp/checkpoint

#可以是绝对路径,也可以是相对此配置文件的相对路径
#相对路径会自动补全
spark.lib.path=lib

######################################################
#                                                    #
#                spark self config                   #
#                                                    #
######################################################
#执行集群设置,不用设置,一般使用YARN
spark.master=yarn

#YARN部署模式
#default=cluster
spark.submit.deployMode=cluster

#spark-streaming每个批次间隔时间
#default=300
spark.batch.duration=30

#spark网络序列化方式,默认是JavaSerializer,可针对所有类型但速度较慢
#这里使用推荐的Kryo方式
#kafka-0.10必须使用此方式
spark.serializer=org.apache.spark.serializer.KryoSerializer

#++++++++++++++++++++++Driver节点相关配置+++++++++++++++++++++++++++
#Driver节点使用内存大小设置
#default=1G
spark.driver.memory=512M

#Driver节点使用的cpu个数设置
#default=1
spark.driver.cores=1

#Driver节点构建时spark-jar和user-jar冲突时优先使用用户提供的,这是一个实验性质的参数只对cluster模式有效
#default=false
spark.driver.userClassPathFirst=false

#++++++++++++++++++++++Executor节点相关配置+++++++++++++++++++++++++
#Executor个数设置
#default=1
spark.executor.instances=1

#Executor使用cpu个数设置
#default=1
spark.executor.cores=1

#Executor使用内存大小设置
#default=1G
spark.executor.memory=512M

#同driver节点配置作用相同,但是是针对executor的
#default=false
spark.executor.userClassPathFirst=true

#++++++++++++++++++++++++Executor动态分配相关配置++++++++++++++++++++
#Executor动态分配的前置服务
#default=false
spark.shuffle.service.enabled=true

#服务对应的端口,此端口服务是配置在yarn-site中的,由NodeManager服务加载启动
#default=7337
spark.shuffle.service.port=7337

#配置是否启用资源动态分配,此动态分配是针对executor的,需要yarn集群配置支持动态分配
#default=false
spark.dynamicAllocation.enabled=true

#释放空闲的executor的时间
#default=60s
spark.dynamicAllocation.executorIdleTimeout=60s

#有缓存的executor空闲释放时间
#default=infinity(默认不释放)
#spark.dynamicAllocation.cachedExecutorIdleTimeout=

#初始化executor的个数,如果设置executor-number,谁小用谁
#default=minExecutors(不设置使用此项配置值)
spark.dynamicAllocation.initialExecutors=1

#executor动态分配可分配最大数量
#default=infinity
spark.dynamicAllocation.maxExecutors=60

#executor动态收缩的最小数量
#default=0
spark.dynamicAllocation.minExecutors=1

#批次调度延迟多长时间开始增加executor
#default=1s
spark.dynamicAllocation.schedulerBacklogTimeout=1s

#同上,但是是针对之后的请求
#default=SchedulerBacklogTimeout(不设置使用此项配置值)
spark.dynamicAllocation.sustainedSchedulerBacklogTimeout=1s

######################################################
#                                                    #
#                  spark process                     #
#                  Source config                     #
#                                                    #
######################################################
#
#spark.source.kafka.consume 后的配置是kafka标准配置,指定之后,将会自动传递给kafkaConsumer
#
#
spark.source.kafka.consume.topics=hello_spark
#
spark.source.kafka.consume.group.id=kafka.consumer.0018
#
spark.source.kafka.consume.bootstrap.servers=kafka1:9092,kafka2:9092
spark.source.kafka.consume.auto.offset.reset=earliest
#offset.store.type是指定如何管理offset的.不指定则匿名消费,kafka表示由kafka自己管理,另有redis和hbase的管理方式
spark.source.kafka.offset.store.type=kafka
#使用redis管理offset时的配置依赖项
#spark.source.kafka.offset.store.type=redis
#spark.source.kafka.offset.store.redis.hosts=localhost
#spark.source.kafka.offset.store.redis.port=6379
#
spark.source.kafka.consume.key.deserializer=org.apache.kafka.common.serialization.StringDeserializer
spark.source.kafka.consume.value.deserializer=org.apache.kafka.common.serialization.StringDeserializer
spark.source.kafka.consume.max.partition.fetch.bytes=10485760
spark.source.kafka.consume.fetch.max.wait.ms=3000
#
#
######################################################
#                                                    #
#                  spark process                     #
#                   Sink config                      #
#                                                    #
######################################################
#目前暂不支持自动的sink配置,此配置需用户在自己的代码里通过sparkConf.get("spark.sink.hdfs.path")的方式获取使用
spark.sink.hdfs.path=/tmp/test
spark.sink.redis.host=192.168.1.1
spark.sink.redis.port=6379
spark.sink.redis.db=0
spark.sink.redis.timeout=30


这只是简单的示例配置，更详细的配置参考后文中介绍的产生配置的脚本


```
  
  
#### 3.创建工程骨架  
  
可使用spark-cli 中的create_app.sh创建默认配置文件,使用方法,运行create_app.sh即可,按提示输入

```scala    
package com.streamxhub.spark.hello

import org.apache.spark.streaming.StreamingContext
import com.streamxhub.spark.core.Streaming
import com.streamxhub.spark.core.support.kafka.KafkaDirectSource

object HelloApp extends Streaming {
    override def handle(ssc : StreamingContext): Unit = {
        val source = new KafkaDirectSource[String,String](ssc)
        //val conf = ssc.sparkContext.getConf
        source.getDStream[(String,String)](m => (m.topic,m.value)).foreachRDD((rdd,time) => {
            rdd.take(10).foreach(println)
            source.updateOffsets(time.milliseconds)
        })
    }
}


```

#### 4.工程结构

```
    上一步会创建一个项目,编译完会有个tar.gz包出现,解开包工程结构如下:
    ${artifactId}/
        bin/ (启动停止脚本)
            setclasspath.sh
            shutdown.sh
            spark.sh
            startup.sh
        lib/ (项目核心jar包)
            xxx.jar
        conf/ (项目配置文件)
            prod/${artifactId}.properties    
        logs/ (日志目录)
            
        temp/ (零时文件目录)          
    
```

#### 5.启动停止项目
     $bash bin/startup.sh ../conf/prod/${artifactId}.properties
     $bash bin/shutdown.sh

##### 5.1.框架说明
```
    1) spark-cli
        -----脚手架自动创建项目骨架和配置的工程模块
        ----create_conf.sh
            ----[产生StreamX-Spark以及Spark自身需要的一些参数]
            ----[包括startup.sh需要的参数以及相关一些组建需要的参数]
            ----[更详细的内容可以看脚本自身,其中包含了说明]
        ----create_app.sh
            ----[创建模版项目,需要一个路径,将会在这个路径下创建模版项目]
            ----[模版项目包含一个父级pom文件以及一个子模块和模块需要的pom和assembly文件及相关目录结构]      
        bin/
            startup.sh
                ----[提交spark任务的脚本,会做一些基础参数检查和生成]
                ----[需要的参数是properties类型的文件,可由上面的脚本产生]
                ----[将startup.sh脚本加入到crontab中即可简单实现失败重启]
                ----[脚本自带防重复启动的功能]
            shutdown.sh
                ----[停止spark任务脚本,如检查到多个app,会弹框提示]    
       
     
    2) spark-core
        -----spark极速开发核心模块    
        org
        ----apache.spark
            ----streaming[自定义的spark streaming一些Listenter]
                ----CongestionMonitorListener 拥堵监控
                    ----[会对streaming任务是否产生拥堵进行告警和停止任务]
                ----JobInfoReportListener 任务信息记录
                    ----[会对streaming对任务详细信息进行记录输出到对应的kafka中]
                ----自定义及使用方法见后文的 5.2 Streaming Listener内容
        ----com.streamxhub.spark.core[StreamX-Spark的核心架构代码]
            ----channel[通道组件]
                ----[目前只有接口没有具体实现]
            ----serializable[输入输出格式化序列组件]
            ----util[工具类]
            ----sink[输出组件]
                ----[目前有influx,kafka,mysql,redis,show等]
            ----sources[spark streaming数据源组件]
                ----[目前只有接口没有具体实现]
            ----support[支持类]
                ----hbase[hbase连接池相关组件]
                ----kafka[kafka生产消费相关组件]
                    ----manager[offset管理相关]
                    ----writer[写入kafka相关]
                ----redis[redis连接池相关组件]
            ----Streaming[用户需继承实现的特质]
                ----init
                    ----[初始化SparkConf的函数,用户可做一些自定义初始化动作]
                    ----[函数会在handle之前执行]
                    ----[参数是SparkConf]
                    ----[返回值是Unit]
                ----handle
                    ----[用户可将计算逻辑实现在这个函数里]
                    ----[函数会在main函数的最后执行]
                    ----[参数是StreamingContext]
                    ----[返回值是Unit]
          
    3) spark-monitor
        -----预留模块,将来做spark任务监控使用...          
```


