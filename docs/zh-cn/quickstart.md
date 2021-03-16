# 快速开始
> 本章节将从项目的编译到第一个项目的运行,进行详细的说明

## 起步
千里之行始于足下,从这一刻开始即将进入StreamX开发的奇妙旅程,我们要做的第一件事就是将项目clone到本地,执行编译,在编译前请确保以下事项
- 确保本机安装的JDK是`1.8`及以上的版本
- 确保本机已经安装了maven

如果准备就绪,就可以clone项目并且执行编译了

```git
git clone https://github.com/streamxhub/streamx.git
cd streamx
mvn clean install -DskipTests
```

顺利的话就会看到编译成功.

```log
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for StreamX 1.0.0:
[INFO]
[INFO] StreamX ............................................ SUCCESS [  1.882 s]
[INFO] StreamX : Common ................................... SUCCESS [ 15.700 s]
[INFO] StreamX : Flink Parent ............................. SUCCESS [  0.032 s]
[INFO] StreamX : Flink Common ............................. SUCCESS [  8.243 s]
[INFO] StreamX : Flink Core ............................... SUCCESS [ 17.332 s]
[INFO] StreamX : Flink Test ............................... SUCCESS [ 42.742 s]
[INFO] StreamX : Spark Parent ............................. SUCCESS [  0.018 s]
[INFO] StreamX : Spark Core ............................... SUCCESS [ 12.028 s]
[INFO] StreamX : Spark Test ............................... SUCCESS [  5.828 s]
[INFO] StreamX : Spark Cli ................................ SUCCESS [  0.016 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  01:43 min
[INFO] Finished at: 2021-03-15T17:02:22+08:00
[INFO] ------------------------------------------------------------------------
```

## 开发
从这里开始第一个streamx程序,从最简单的需求入手,这里的需求是从`kafka`读取用户数据写入`mysql`
### 需求
* 从kafka里读取用户的数据写入到mysql中
* 只要年龄小于30岁的数据
* `kafka` 的 `topic`为`test_user` 数据格式如下:

```json
{
  "name" : "$name",
  "age": $age,
  "gender": $gender,
  "address": "$address"
}
```

* 要写入的`mysql`的表为`t_user`,表结构如下:

```bash 
create table user(
    `name` varchar(32),
    `age` int(3),
    `gender` int(1),
    `address` varchar(255)
)
```

### 代码
用streamx开发一个这样的需求非常简单,只需要几行代码就搞定了

```scala
package com.streamxhub.streamx.quickstart

import com.streamxhub.streamx.common.util.JsonUtils
import com.streamxhub.streamx.flink.core.scala.FlinkStreaming
import com.streamxhub.streamx.flink.core.scala.sink.JdbcSink
import com.streamxhub.streamx.flink.core.scala.source.KafkaSource
import org.apache.flink.api.scala._

object QuickStartApp extends FlinkStreaming {

  override def handle(): Unit = {
    val source = KafkaSource()
      .getDataStream[String]()
      .map(x => JsonUtils.read[User](x.value))
      .filter(_.age < 30)

    JdbcSink().towPCSink[User](source)(user =>
      s"""
        |insert into t_user(`name`,`age`,`gender`,`address`)
        |value('${user.name}',${user.age},${user.gender},'${user.address}')
        |""".stripMargin
    )
  }

}

case class User(name: String, age: Int, gender: Int, address: String)

```
?> 项目工程和代码已经提供好了,位于 [streamx-flink-quickstart](https://github.com/streamxhub/streamx/streamx-flink/streamx-flink-quickstart) 开箱即用,不需要写一行代码

![](http://assets.streamxhub.com/1615863880697.jpg)


## 配置
`kafka` 和 `mysql`相关准备工作就绪了,接下来要改项目的配置,找到项目路径下的`assembly/conf/application.yml`
这里有很多配置项,先不用管,也不用改,现在只关注`kafka`和`mysql`两项配置
```yaml
# kafka source
kafka.source:
  bootstrap.servers: kfk1:9092,kfk2:9092,kfk3:9092
  topic: test_user
  group.id: user_01
  auto.offset.reset: earliest

# mysql
mysql:
  driverClassName: com.mysql.cj.jdbc.Driver
  jdbcUrl: jdbc:mysql://localhost:3306/test?useSSL=false&allowPublicKeyRetrieval=true
  username: root
  password: 123456
```  
找到上面两项配置修改相关的 `kafka` 和 `mysql` 信息即可.

!> 配置文件是非常重要的概念,会在后续章节中进行详细说明

## 测试

准备工作完毕现在启动项目进行本地测试,找到项目主类`com.streamxhub.streamx.quickstart.QuickStartApp` 运行启动,并且加上参数
`--app.conf $path`,path即为当前项目的配置文件的路径(上面修改的配置文件)如不指定该参数会报错如下:
```log
Exception in thread "main" java.lang.ExceptionInInitializerError: 
[StreamX] Usage:can't fond config,please set "--app.conf $path " in main arguments
	at com.streamxhub.streamx.flink.core.scala.util.FlinkStreamingInitializer.initParameter(FlinkStreamingInitializer.scala:126)
	at com.streamxhub.streamx.flink.core.scala.util.FlinkStreamingInitializer.<init>(FlinkStreamingInitializer.scala:85)
	at com.streamxhub.streamx.flink.core.scala.util.FlinkStreamingInitializer$.initStream(FlinkStreamingInitializer.scala:55)
	at com.streamxhub.streamx.flink.core.scala.FlinkStreaming$class.main(FlinkStreaming.scala:94)
	at com.streamxhub.streamx.quickstart.QuickStartApp$.main(QuickStartApp.scala:9)
	at com.streamxhub.streamx.quickstart.QuickStartApp.main(QuickStartApp.scala)

```
看到如下信息说明启动成功
```log


                         ▒▓██▓██▒
                     ▓████▒▒█▓▒▓███▓▒
                  ▓███▓░░        ▒▒▒▓██▒  ▒
                ░██▒   ▒▒▓▓█▓▓▒░      ▒████
                ██▒         ░▒▓███▒    ▒█▒█▒
                  ░▓█            ███   ▓░▒██
                    ▓█       ▒▒▒▒▒▓██▓░▒░▓▓█
                  █░ █   ▒▒░       ███▓▓█ ▒█▒▒▒
                  ████░   ▒▓█▓      ██▒▒▒ ▓███▒
               ░▒█▓▓██       ▓█▒    ▓█▒▓██▓ ░█░
         ▓░▒▓████▒ ██         ▒█    █▓░▒█▒░▒█▒
        ███▓░██▓  ▓█           █   █▓ ▒▓█▓▓█▒
      ░██▓  ░█░            █  █▒ ▒█████▓▒ ██▓░▒
     ███░ ░ █░          ▓ ░█ █████▒░░    ░█░▓  ▓░
    ██▓█ ▒▒▓▒          ▓███████▓░       ▒█▒ ▒▓ ▓██▓
 ▒██▓ ▓█ █▓█       ░▒█████▓▓▒░         ██▒▒  █ ▒  ▓█▒
 ▓█▓  ▓█ ██▓ ░▓▓▓▓▓▓▓▒              ▒██▓           ░█▒
 ▓█    █ ▓███▓▒░              ░▓▓▓███▓          ░▒░ ▓█
 ██▓    ██▒    ░▒▓▓███▓▓▓▓▓██████▓▒            ▓███  █
▓███▒ ███   ░▓▓▒░░   ░▓████▓░                  ░▒▓▒  █▓
█▓▒▒▓▓██  ░▒▒░░░▒▒▒▒▓██▓░                            █▓
██ ▓░▒█   ▓▓▓▓▒░░  ▒█▓       ▒▓▓██▓    ▓▒          ▒▒▓
▓█▓ ▓▒█  █▓░  ░▒▓▓██▒            ░▓█▒   ▒▒▒░▒▒▓█████▒
 ██░ ▓█▒█▒  ▒▓▓▒  ▓█                █░      ░░░░   ░█▒
 ▓█   ▒█▓   ░     █░                ▒█              █▓
  █▓   ██         █░                 ▓▓        ▒█▓▓▓▒█░
   █▓ ░▓██░       ▓▒                  ▓█▓▒░░░▒▓█░    ▒█
    ██   ▓█▓░      ▒                    ░▒█▒██▒      ▓▓
     ▓█▒   ▒█▓▒░                         ▒▒ █▒█▓▒▒░░▒██
      ░██▒    ▒▓▓▒                     ▓██▓▒█▒ ░▓▓▓▓▒█▓
        ░▓██▒                          ▓░  ▒█▓█  ░░▒▒▒
            ▒▓▓▓▓▓▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒░░▓▓  ▓░▒█░

                 +----------------------+
                 +  十步杀一人，千里不留行  +
                 +  事了拂衣去，深藏功与名  +
                 +----------------------+

              [StreamX] let's flink|spark easy ô‿ô!



[StreamX] FlinkStreaming StreamX QuickStart App Starting...
2021-03-15 23:28:02 | INFO  | main | org.apache.flink.runtime.taskexecutor.TaskExecutorResourceUtils | The configuration option taskmanager.cpu.cores required for local execution is not set, setting it to the maximal possible value.
2021-03-15 23:28:02 | INFO  | main | org.apache.flink.runtime.taskexecutor.TaskExecutorResourceUtils | The configuration option taskmanager.memory.task.heap.size required for local execution is not set, setting it to the maximal possible value.
2021-03-15 23:28:02 | INFO  | main | org.apache.flink.runtime.taskexecutor.TaskExecutorResourceUtils | The configuration option taskmanager.memory.task.off-heap.size required for local execution is not set, setting it to the maximal possible value.
2021-03-15 23:28:02 | INFO  | main | org.apache.flink.runtime.taskexecutor.TaskExecutorResourceUtils | The configuration option taskmanager.memory.network.min required for local execution is not set, setting it to its default value 64 mb.
2021-03-15 23:28:02 | INFO  | main | org.apache.flink.runtime.taskexecutor.TaskExecutorResourceUtils | The configuration option taskmanager.memory.network.max required for local execution is not set, setting it to its default value 64 mb.
2021-03-15 23:28:02 | INFO  | main | org.apache.flink.runtime.taskexecutor.TaskExecutorResourceUtils | The configuration option taskmanager.memory.managed.size required for local execution is not set, setting it to its default value 128 mb.
2021-03-15 23:28:02 | INFO  | main | org.apache.flink.runtime.minicluster.MiniCluster | Starting Flink Mini Cluster

```
## 部署
当程序开发完成后,本地测试通过后,需要往生成环境发布,