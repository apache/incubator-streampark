---
title: 'Jdbc Connector'
name: 'Jdbc Connector'
author: 'benjobs'
time: 2020/03/24
original: true
---

Flink 官方 提供了[JDBC](https://ci.apache.org/projects/flink/flink-docs-release-1.12/dev/connectors/jdbc.html)的连接器,用于从 JDBC 中读取或者向其中写入数据,可提供 ==AT_LEAST_ONCE== (至少一次)的处理语义

`StreamX`中基于两阶段提交实现了 ==EXACTLY_ONCE== (精确一次)语义的`JdbcSink`,并且采用[`光 HikariCP`](https://github.com/brettwooldridge/HikariCP)为连接池,让数据的读取和写入更简单更准确

## Jdbc 信息配置

在`StreamX`中`Jdbc Connector`的实现用到了[`光 HikariCP`](https://github.com/brettwooldridge/HikariCP)连接池,相关的配置在`jdbc`的namespace下,约定的配置如下:

```yaml
jdbc:
  semantic: EXACTLY_ONCE # EXACTLY_ONCE|AT_LEAST_ONCE|NONE
  username: root
  password: 123456
  driverClassName: com.mysql.jdbc.Driver
  connectionTimeout: 30000
  idleTimeout: 30000
  maxLifetime: 30000
  maximumPoolSize: 6
  jdbcUrl: jdbc:mysql://localhost:3306/test?useSSL=false&allowPublicKeyRetrieval=true
```

### semantic 语义配置

`semantic`这个参数是在`JdbcSink`写时候的语义,仅对 ==`JdbcSink`== 有效,`JdbcSource`会自动屏蔽该参数,有三个可选项

<div class="counter">

* EXACTLY_ONCE
* AT_LEAST_ONCE
* NONE

</div>

#### EXACTLY_ONCE

如果`JdbcSink`配置了 `EXACTLY_ONCE`语义,则底层采用了两阶段提交的实现方式来完成写入,此时要flink配合开启`Checkpointing`才会生效,如何开启checkpoint请参考第二章关于[checkpoint]()配置部分

#### AT_LEAST_ONCE && NONE

默认不指定会采用`NONE`语义,这两种配置效果一样,都是保证 ==至少一次== 语义

::: tip 提示
开启`EXACTLY_ONCE`精确一次的好处是显而易见的,保证了数据的准确性,但成本也是高昂的,需要`checkpoint`的支持,底层模拟了事务的提交读,对实时性有一定的损耗,如果你的业务对数据的准确性要求不是那么高,则建议采用`AT_LEAST_ONCE`语义
:::

### 其他配置

除了特殊的`semantic` 配置项之外,其他的所有的配置都必须遵守 ==`光 HikariCP`== 连接池的配置,具体可配置项和各个参数的作用请参考`光 HikariCP`[官网文档](https://github.com/brettwooldridge/HikariCP#gear-configuration-knobs-baby).


## Jdbc 读取数据

在`StreamX`中`JdbcSource`用来读取数据,并且根据数据的`offset`做到数据读时可回放,我们看看具体如何用`JdbcSource`读取数据,假如需求如下

<div class="counter">

* 从`t_order`表中读取数据,以`timestamp`字段为参照,起始值为`2020-12-16 12:00:00`往后抽取数据
* 将读取到的数据构造成`Order`对象返回

</div>

jdbc配置和读取代码如下

<CodeGroup>
<CodeGroupItem title="配置" active>

```yaml
jdbc:
  driverClassName: com.mysql.jdbc.Driver
  jdbcUrl: jdbc:mysql://localhost:3306/test?useSSL=false&allowPublicKeyRetrieval=true
  username: root
  password: 123456
```

</CodeGroupItem>
<CodeGroupItem title="scala">

```scala
import com.streamxhub.streamx.flink.core.scala.FlinkStreaming
import com.streamxhub.streamx.flink.core.scala.source.JdbcSource
import org.apache.flink.api.scala._

object MySQLSourceApp extends FlinkStreaming {

  override def handle(): Unit = {

    JdbcSource().getDataStream[Order](lastOne => {
      //防止抽取过于密集,间隔5秒抽取一次数据                          
      Thread.sleep(5000);
      val laseOffset = if (lastOne == null) "2020-12-16 12:00:00" else lastOne.timestamp
      s"select * from t_order where timestamp > '$laseOffset' order by timestamp asc "
    },
      _.map(x => new Order(x("market_id").toString, x("timestamp").toString))
    ).print()

  }

}

class Order(val marketId: String, val timestamp: String) extends Serializable
```

</CodeGroupItem>
<CodeGroupItem title="java">

```java 
import com.streamxhub.streamx.flink.core.java.function.SQLQueryFunction;
import com.streamxhub.streamx.flink.core.java.function.SQLResultFunction;
import com.streamxhub.streamx.flink.core.java.function.StreamEnvConfigFunction;
import com.streamxhub.streamx.flink.core.java.source.JdbcSource;
import com.streamxhub.streamx.flink.core.scala.StreamingContext;
import com.streamxhub.streamx.flink.core.scala.util.StreamEnvConfig;
import org.apache.flink.api.common.typeinfo.TypeInformation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MySQLJavaApp {

    public static void main(String[] args) {
        StreamEnvConfig envConfig = new StreamEnvConfig(args, null);
        StreamingContext context = new StreamingContext(envConfig);
        new JdbcSource<Order>(context)
                .getDataStream(
                        (SQLQueryFunction<Order>) lastOne -> {
                            //防止抽取过于密集,间隔5秒抽取一次数据                          
                            Thread.sleep(5000);
                            
                            Serializable lastOffset = lastOne == null 
                            ? "2020-12-16 12:00:00" 
                            : lastOne.timestamp;
                            
                            return String.format(
                                "select * from t_order " +
                                "where timestamp > '%s' " +
                                "order by timestamp asc ",
                                lastOffset
                            );
                        },
                        (SQLResultFunction<Order>) iterable -> {
                            List<Order> result = new ArrayList<>();
                            iterable.forEach(item -> {
                                Order Order = new Order();
                                Order.marketId = item.get("market_id").toString();
                                Order.timestamp = Long.parseLong(item.get("timestamp").toString());
                                result.add(Order);
                            });
                            return result;
                        })
                .returns(TypeInformation.of(Order.class))
                .print();

        context.start();
    }
}
```
</CodeGroupItem>
</CodeGroup>

以`java` api为例,这里要传入两个参数

<div class="counter">

* `SQLQueryFunction<T> queryFunc`
* `SQLResultFunction<T> resultFunc`

</div>

### queryFunc获取一条sql

`queryFunc`是要传入一个`SQLQueryFunction`类型的`function`,该`function`用于获取查询sql的,会将最后一条记录返回给开发者,然后需要开发者根据最后一条记录返回一条新的查询`sql`,`queryFunc`定义如下:

```java 
/**
 * @author benjobs
 */
@FunctionalInterface
public interface SQLQueryFunction<T> extends Serializable {
    /**
     * 获取要查询的SQL
     *
     * @return
     * @throws Exception
     */
    String query(T last) throws Exception;
}
```

所以上面的代码中,第一次上来`lastOne`(最后一条记录)为null,会判断一下,为null则取需求里默认的`offset`,查询的sql里根据`timestamp`字段正序排,这样在第一次查询之后,会返回最后的那条记录,下次直接可以使用这条记录作为下一次查询的根据

::: info 注意事项
`JdbcSource`实现了`CheckpointedFunction`,即当程序开启 ==`checkpoint`== 后,会将这些诸如`laseOffset`的状态数据保存到`state backend`,这样程序挂了,再次启动会自动从`checkpoint`中恢复`offset`,会接着上次的位置继续读取数据,
一般在生产环境,更灵活的方式是将`lastOffset`写入如`redis`等存储中,每次查询完之后再将最后的记录更新到`redis`,这样即便程序意外挂了,再次启动,也可以从`redis`中获取到最后的`offset`进行数据的抽取,也可以很方便的人为的任意调整这个`offset`进行数据的回放
:::

### resultFunc 处理查询到的数据

`resultFunc`的参数类型是`SQLResultFunction<T>`,是将一个查询到的结果集放到`Iterable<Map<String, ?>>`中返回给开发者,可以看到返回了一个迭代器`Iterable`,迭代器每次迭代返回一个`Map`,该`Map`里记录了一行完整的记录,`Map`的`key`为查询字段,`value`为值,`SQLResultFunction<T>`定义如下

```java 
/**
 * @author benjobs
 */
@FunctionalInterface
public interface SQLResultFunction<T> extends Serializable {
    /**
     * 将查下结果以Iterable<Map>的方式返回,开发者去实现转成对象.
     *
     * @param map
     * @return
     */
    Iterable<T> result(Iterable<Map<String, ?>> iterable);
}
```

## Jdbc 读取写入

`StreamX`中`JdbcSink`是用来写入数据,我们看看具体如何用`JdbcSink`写入数据,假如需求是需要从`kakfa`中读取数据,写入到`mysql`

<CodeGroup>
<CodeGroupItem title="配置" active>

```yaml
kafka.source:
  bootstrap.servers: kfk1:9092,kfk2:9092,kfk3:9092
  pattern: user
  group.id: user_02
  auto.offset.reset: earliest # (earliest | latest)
  ...
  
jdbc:
  semantic: EXACTLY_ONCE # EXACTLY_ONCE|AT_LEAST_ONCE|NONE
  driverClassName: com.mysql.jdbc.Driver
  jdbcUrl: jdbc:mysql://localhost:3306/test?useSSL=false&allowPublicKeyRetrieval=true
  username: root
  password: 123456
```
::: danger 注意事项
配置里`jdbc`下的 ==`semantic`== 是写入的语义,在上面[Jdbc信息配置](#jdbc-信息配置)有介绍,该配置只会在`JdbcSink`下生效,`StreamX`中基于两阶段提交实现了 ==EXACTLY_ONCE== 语义,
这本身需要被操作的数据库(`mysql`,`oracle`,`MariaDB`,`MS SQL Server`)等支持事务,理论上所有支持标准Jdbc事务的数据库都可以做到EXACTLY_ONCE(精确一次)的写入
:::

</CodeGroupItem>
<CodeGroupItem title="scala">

```scala
import com.streamxhub.streamx.common.util.JsonUtils
import com.streamxhub.streamx.flink.core.scala.FlinkStreaming
import com.streamxhub.streamx.flink.core.scala.sink.JdbcSink
import com.streamxhub.streamx.flink.core.scala.source.KafkaSource
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.java.typeutils.TypeExtractor.getForClass
import org.apache.flink.api.scala._
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema

object JdbcSinkApp extends FlinkStreaming {

  override def handle(): Unit = {
        val source = KafkaSource()
          .getDataStream[String]()
          .map(x => JsonUtils.read[User](x.value))
          
        JdbcSink().sink[User](source)(user =>
          s"""
          |insert into t_user(`name`,`age`,`gender`,`address`)
          |value('${user.name}',${user.age},${user.gender},'${user.address}')
          |""".stripMargin
        )  
  }

}

case class User(name:String,age:Int,gender:Int,address:String)

```

</CodeGroupItem>
<CodeGroupItem title="java">

```java 
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamxhub.streamx.flink.core.java.function.StreamEnvConfigFunction;
import com.streamxhub.streamx.flink.core.java.source.KafkaSource;
import com.streamxhub.streamx.flink.core.scala.StreamingContext;
import com.streamxhub.streamx.flink.core.scala.source.KafkaRecord;
import com.streamxhub.streamx.flink.core.scala.util.StreamEnvConfig;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.io.Serializable;

import static org.apache.flink.api.java.typeutils.TypeExtractor.getForClass;

public class JdbcSinkJavaApp {

    public static void main(String[] args) {
        StreamEnvConfig envConfig = new StreamEnvConfig(args, null);
        StreamingContext context = new StreamingContext(envConfig);
        ObjectMapper mapper = new ObjectMapper();

        DataStream<JavaUser> source = new KafkaSource<String>(context)
                .getDataStream()
                .map((MapFunction<KafkaRecord<String>, JavaUser>) value ->
                    mapper.readValue(value.value(), JavaUser.class));

        new JdbcSink<JavaUser>(context)
                .sql((SQLFromFunction<JavaUser>) JavaUser::toSql)
                .sink(source);
                
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

### 根据数据流生成目标SQL

在写入的时候,需要知道具体写入的`sql`语句,该`sql`语句需要开发者通过`function`的方式提供,在`scala` api中,直接在`sink`方法后跟上`function`即可,`java` api 则是通过`sql()`方法传入一个`SQLFromFunction`类型的`function`

下面以`java` api为例说明,我们来看看`java`api 中提供sql的`function`方法的定义

```java 
/**
 * @author benjobs
 */
@FunctionalInterface
public interface SQLFromFunction<T> extends Serializable {
    /**
     * @param bean
     * @return
     */
    String from(T bean);
}

```

`SQLFromFunction`上的泛型`<T>`即为`DataStream`里实际的数据类型,该`function`里有一个方法`form(T bean)`,这个`bean`即为当前`DataStream`中的一条具体数据,会将该数据返给开发者,开发者来决定基于这条数据,生成一条具体可以往数据库中插入的`sql`


### 设置写入批次大小

在 非 `EXACTLY_ONCE`(精确一次的语义下)可以适当的设置`batch.size`来提高Jdbc写入的性能(前提是业务允许的情况下),具体配置如下

```yaml
jdbc:
  semantic: EXACTLY_ONCE # EXACTLY_ONCE|AT_LEAST_ONCE|NONE
  driverClassName: com.mysql.jdbc.Driver
  jdbcUrl: jdbc:mysql://localhost:3306/test?useSSL=false&allowPublicKeyRetrieval=true
  username: root
  password: 123456
  batch.size: 1000
```
这样一来就不是来一条数据就立即写入,而是积攒一个匹配然后执行批量插入

::: danger 注意事项
这个设置仅在非`EXACTLY_ONCE`语义下生效,带来的好处是可以提高Jdbc写入的性能,一次大批量的插入数据,缺点是数据写入势必会有延迟,请根据实际使用情况谨慎使用
:::

## 多实例Jdbc支持


## 手动指定Jdbc连接信息
