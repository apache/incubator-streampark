# StreamX
let't bigdata easy
    
### BUILD和使用方法
#### 1.构建方法

```
    $git clone https://github.com/streamxhub/StreamX.git
    $cd StreamX
    $mvn clean install

    maven构建时已默认在pom中跳过了所有的test代码的编译和测试
    构建完成之后会将jar安装到m2的对应路径下，使用时在自己的项目的pom.xml文件里添加
    
    <dependency>
        <groupId>com.streamxhub.spark</groupId>
    	<artifactId>spark-core</artifactId>
    	<version>1.0.0</version>
    </dependency>
```

#### 2.配置说明

```

可使用spark-cli 中的create_conf.sh创建默认配置文件,使用方法:
$bash 中的create_conf.sh >my.properties


生成的标准配置内容
此配置文件可以自定义添加任何以spark开头的配置参数,spark程序本身的依赖参数也在此配置文件配置

######################################################
#                                                    #
#               spark process run.sh                 #
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
spark.source.kafka.consume.topics=homework_sync_canal
#
spark.source.kafka.consume.group.id=z.cloud.kafka.consumer.0018
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
  
#### 3.示例代码  
  
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

#### 4.运行示例代码

```
    使用bin中的create_conf.sh创建标准配置文件
    
    $bash create_conf.sh >my.properties
    
    创建配置文件之后按配置文件按提示设置必须的参数
    
    spark.app.main=com.streamhub.spark.hello.HelloApp
    
    设置完成后使用script中的run.sh来提交和停止任务
    启动
    $bash run.sh my.properties
    停止(任意给第二个参数即可kill掉spark任务)
    $bash run.sh my.properties stop
    
```

#### 5.API 说明
##### 5.1.框架说明
```
    org
    ----apache.spark
        ----streaming[自定义的spark streaming一些Listenter]
            ----CongestionMonitorListener 拥堵监控
                ----[会对streaming任务是否产生拥堵进行告警和停止任务]
            ----JobInfoReportListener 任务信息记录
                ----[会对streaming对任务详细信息进行记录输出到对应的kafka中]
            ----自定义及使用方法见后文的 5.2 Streaming Listener内容
        ----RpcDemo里是spark RPC服务和client实现的演示代码
            ----对应的启动代码在resources里
            ----spark-rpcdemo client启动代码放到spark/bin下即可使用
            ----start-rpcdemo-server.sh 放到spark/sbin下既可以使用
    ----com.streamxhub.spark.core[StreamX-Spark的核心架构代码]
        ----channel[通道组建]
            ----[目前只有接口没有具体实现]
        ----serializable[输入输出格式化序列组建]
        ----kit[工具类]
        ----support[支持类]
            ----hbase[hbase连接池相关组建]
            ----kafka[kafka生产消费相关组建]
                ----manager[offset管理相关]
                ----writer[写入kafka相关]
            ----redis[redis连接池相关组建]
        ----sink[输出组建]
            ----[目前有influx,kafka,mysql,redis,show等]
        ----sources[spark streaming数据源组建]
            ----[目前只有接口没有具体实现]
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
    script[部署相关的脚本]
        ----create_default_conf.sh
            ----[产生FireSpark以及Spark自身需要的一些参数]
            ----[包括run.sh需要的参数以及相关一些组建需要的参数]
            ----[更详细的内容可以看脚本自身,其中包含了说明]
        ----run.sh
            ----[提交spark任务的脚本,会做一些基础参数检查和生成]
            ----[需要的参数是properties类型的文件,可由上面的脚本产生]
            ----[将run脚本加入到crontab中即可简单实现失败重启]
            ----[脚本自带防重复启动的功能]
        ----create_template_project.sh
            ----[创建模版项目,需要一个路径,将会在这个路径下创建模版项目]
            ----[模版项目包含一个父级pom文件以及一个子模块和模块需要的pom和assembly文件及相关目录结构]
```


