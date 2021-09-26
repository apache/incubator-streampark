---
title: '快速开始'
sidebar: true
author: 'benjobs'
date: 2020/03/20
original: true
---

千里之行始于足下,从这一刻开始即将进入`StreamX` 开发的奇妙旅程,准备好了吗? 让我们开始吧

## 起步

我们要做的第一件事就是将项目clone到本地,执行编译,在编译前请确保以下事项

<div class="counter">

* 确保本机安装的JDK`1.8`及以上的版本
* 确保本机已经安装了maven

</div>

如果准备就绪,就可以clone项目并且执行编译了

```bash
git clone https://github.com/streamxhub/streamx.git
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

## DataStream

从这里开始用StreamX开发第一个`DataStream` 程序,从最简单的需求入手,这里的需求是从`kafka`读取用户数据写入`mysql`,需求如下:

<div class="counter">

* 从kafka里读取用户的数据写入到mysql中
* 只要年龄小于30岁的用户数据
* `kafka` 的 `topic`为`test_user`
* 要写入的`mysql`的表为`t_user`

</div>

`kafka`的`topic` 的数据格式如下
```json
{
  "name" : "$name",
  "age" : $age,
  "gender" : $gender,
  "address" : "$address"
}
```
`mysql`表`t_user`结构如下:

```sql 
create table user(
    `name` varchar(32),
    `age` int(3),
    `gender` int(1),
    `address` varchar(255)
)
```

### 编码开发

用Streamx开发一个这样的需求非常简单,只需要几行代码就搞定了

<CodeGroup>
<CodeGroupItem title="scala" active>

```scala
package com.streamxhub.streamx.quickstart

import com.streamxhub.streamx.common.util.JsonUtils
import com.streamxhub.streamx.flink.core.scala.FlinkStreaming
import com.streamxhub.streamx.flink.core.scala.sink.JdbcSink
import com.streamxhub.streamx.flink.core.scala.source.KafkaSource
import org.apache.flink.api.scala._
/**
 * @author benjobs
 */
object QuickStartApp extends FlinkStreaming {

  override def handle(): Unit = {
    val source = KafkaSource()
      .getDataStream[String]()
      .map(x => JsonUtils.read[User](x.value))
      .filter(_.age < 30)

    JdbcSink().sink[User](source)(user =>
    s"""
        |insert into t_user(`name`,`age`,`gender`,`address`)
        |value('${user.name}',${user.age},${user.gender},'${user.address}')
        |""".stripMargin
    )
  }
}

case class User(name: String, age: Int, gender: Int, address: String)
```
</CodeGroupItem>
<CodeGroupItem title="java">

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamxhub.streamx.flink.core.java.function.SQLFromFunction;
import com.streamxhub.streamx.flink.core.java.function.StreamEnvConfigFunction;
import com.streamxhub.streamx.flink.core.java.sink.JdbcSink;
import com.streamxhub.streamx.flink.core.java.source.KafkaSource;
import com.streamxhub.streamx.flink.core.scala.StreamingContext;
import com.streamxhub.streamx.flink.core.scala.source.KafkaRecord;
import com.streamxhub.streamx.flink.core.scala.util.StreamEnvConfig;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.streaming.api.datastream.DataStream;

import java.io.Serializable;

/**
 * @author benjobs
 */
public class QuickStartJavaApp {

    public static void main(String[] args) {

        StreamEnvConfig envConfig = new StreamEnvConfig(args, null);

        StreamingContext context = new StreamingContext(envConfig);

        ObjectMapper mapper = new ObjectMapper();

        DataStream<JavaUser> source = new KafkaSource<String>(context)
                .getDataStream()
                .map((MapFunction<KafkaRecord<String>, JavaUser>) value ->
                        mapper.readValue(value.value(), JavaUser.class))
                .filter((FilterFunction<JavaUser>) value -> value.age < 30);

        new JdbcSink<JavaUser>(context)
                .sql((SQLFromFunction<JavaUser>) JavaUser::toSql)
                .towPCSink(source);

        context.start();
    }

}

class JavaUser implements Serializable {
    String name;
    Integer age;
    Integer gender;
    String address;

    public String toSql() {
        return String.format(
                "insert into t_user(`name`,`age`,`gender`,`address`) value('%s',%d,%d,'%s')",
                name,
                age,
                gender,
                address);
    }

}
```
</CodeGroupItem>
</CodeGroup>

::: tip 提示
项目工程和代码已经提供好了,位于 [streamx-quickstart](https://github.com/streamxhub/streamx-quickstart.git) 开箱即用,不需要写一行代码
:::

### 配置文件
`kafka` 和 `mysql`相关准备工作就绪了,接下来要改项目的配置,找到项目路径下的`assembly/conf/application.yml`
这里有很多配置项,先不用管,也不用改,现在只关注 `kafka` 和 `mysql` 两项配置
```yaml
# kafka source
kafka.source:
  bootstrap.servers: kfk1:9092,kfk2:9092,kfk3:9092
  topic: test_user
  group.id: user_01
  auto.offset.reset: earliest

# jdbc
jdbc:
  semantic: EXACTLY_ONCE
  driverClassName: com.mysql.cj.jdbc.Driver
  jdbcUrl: jdbc:mysql://localhost:3306/test?useSSL=false&allowPublicKeyRetrieval=true
  username: root
  password: 123456
```  
找到上面两项配置修改相关的 `kafka` 和 `mysql` 信息即可.

::: tip 提示
`配置`是非常重要的概念,会在下一章节进行介绍说明
:::


### 本地测试

准备工作完毕现在启动项目进行本地测试,找到项目主类`com.streamxhub.streamx.quickstart.QuickStartApp` 运行启动,并且加上参数
`--conf $path`,path即为当前项目的配置文件的路径(上面修改的配置文件),如果没啥意外的话,项目就会在本地启动成功,会看到`kafka`里的数据按照需求成功的写入到`mysql`中
::: warning 注意事项
项目启动时配置文件必须要指定,如不指定该参数会报错如下:
```log
Exception in thread "main" java.lang.ExceptionInInitializerError: 
[Streamx] Usage:can't fond config,please set "--conf $path " in main arguments
	at com.streamxhub.streamx.flink.core.scala.util.FlinkStreamingInitializer.initParameter(FlinkStreamingInitializer.scala:126)
	at com.streamxhub.streamx.flink.core.scala.util.FlinkStreamingInitializer.<init>(FlinkStreamingInitializer.scala:85)
	at com.streamxhub.streamx.flink.core.scala.util.FlinkStreamingInitializer$.initStream(FlinkStreamingInitializer.scala:55)
	at com.streamxhub.streamx.flink.core.scala.FlinkStreaming$class.main(FlinkStreaming.scala:94)
	at com.streamxhub.streamx.quickstart.QuickStartApp$.main(QuickStartApp.scala:9)
	at com.streamxhub.streamx.quickstart.QuickStartApp.main(QuickStartApp.scala)

```
:::

### 部署上线
当程序开发完成并且通过本地测试后,需要往测试或生产环境发布,一般的方式是将jar包上传到集群的某一台部署机,进行`flink run`命令启动项目,然后跟上各种资源参数,
虽然flink启动的时候官方提供了很多短参数的方式进行设置(-c -m -jar等),但是这种方式,不仅可读性差,还极容易出错,稍不注意一个参数不合法就会导致任务启动失败,
在StreamX里这些都简化了,StreamX启动项目如下

<div class="counter">

* 解项目压缩包
* 执行启动脚本

</div>

### 项目结构
解包`Streamx-flink-quickstart-1.0.0.tar.gz`后项目的目录结构如下:
```text
.
Streamx-flink-quickstart-1.0.0
├── bin
│   ├── startup.sh                             //启动脚本  
│   ├── setclasspath.sh                        //java环境变量相关的脚本(内部使用的,用户无需关注)
│   ├── shutdown.sh                            //任务停止脚本(不建议使用)
│   ├── flink.sh                               //启动时内部使用到的脚本(内部使用的,用户无需关注)
├── conf                           
|   ├── application.yaml                       //项目的配置文件
├── lib
│   └── Streamx-flink-quickstart-1.0.0.jar     //项目的jar包
└── temp
```

### 启动命令

启动之前要确定`conf/application.yaml`下的配置文件,在当前`Streamx-flink-quickstart`项目里都已经配置好了,不需改动,直接启动项目即可:
```bash 
bin/startup.sh --conf conf/application.yaml 
```

## Flink Sql

从这里开始用StreamX开发第一个Flink Sql 程序,利用Flink内置的 `DataGen`和`print` Connectors 来完成

::: tip 提示
项目工程和代码已经提供好了,位于 [streamx-quickstart](https://github.com/streamxhub/streamx-quickstart.git) 开箱即用,不需要写一行代码
:::

### 编码开发

下面的代码演示了如何用`scala`和`java`两种api开发这样一个需求,全部代码如下

<CodeGroup>
<CodeGroupItem title="scala" active>

```scala 
package com.streamxhub.streamx.test.tablesql
import com.streamxhub.streamx.flink.core.scala.FlinkStreamTable

object HelloFlinkSQL extends FlinkStreamTable {

  override def handle(): Unit = {
    /**
     * 一行胜千言
     */
    context.sql("myflinksql")
  }
}
```
</CodeGroupItem>
<CodeGroupItem title="java" active>

```java
import com.streamxhub.streamx.flink.core.scala.TableContext;
import com.streamxhub.streamx.flink.core.scala.util.TableEnvConfig;

public class HelloFlinkSQL {

    public static void main(String[] args) {
        TableEnvConfig tableEnvConfig = new TableEnvConfig(args, (tableConfig, parameterTool) -> {
            System.out.println("set tableConfig...");
        });

        TableContext context = new TableContext(tableEnvConfig);
        context.sql("myflinksql");
    }
}

```
</CodeGroupItem>
</CodeGroup>


### 配置文件

找到项目路径下的assembly/conf/sql.yml,内容如下

```sql
myflinksql: |
  CREATE TABLE datagen (
    f_sequence INT,
    f_random INT,
    f_random_str STRING,
    ts AS localtimestamp,
    WATERMARK FOR ts AS ts
  ) WITH (
    'connector' = 'datagen',
    -- optional options --
    'rows-per-second'='5',
    'fields.f_sequence.kind'='sequence',
    'fields.f_sequence.start'='1',
    'fields.f_sequence.end'='1000',
    'fields.f_random.min'='1',
    'fields.f_random.max'='1000',
    'fields.f_random_str.length'='10'
  );

  CREATE TABLE print_table (
    f_sequence INT,
    f_random INT,
    f_random_str STRING
    ) WITH (
    'connector' = 'print'
  );

  INSERT INTO print_table select f_sequence,f_random,f_random_str from datagen;
```

### 本地测试

找到项目主类`com.streamxhub.streamx.quickstart.HelloFlinkSQL` 运行启动,并且加上参数
`--sql $path`,path即为当前项目的sql文件的路径,如果没啥意外的话,项目就会在本地启动成功,生成的数据会打印到控制台
