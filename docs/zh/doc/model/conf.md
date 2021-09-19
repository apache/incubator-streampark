---
title: '项目配置'
sidebar: true
author: 'benjobs'
date: 2020/03/20
original: true
---

配置在`StreamX`中是非常重要的概念,先说说为什么需要配置

## 为什么需要配置

开发`DataStream`程序,大体流程都可以抽象为以下4步

<div class="counter">

- StreamExecutionEnvironment初始并配置
- Source接入数据
- Transformation逻辑处理
- Sink结果数据落地

</div>

![](http://assets.Streamxhub.com/20210317134700.png)

开发`DataStream`程序都需要定义`Environment`初始化并且配置环境相关的参数,一般我们都会在第一步初始化`Environment`并配置各种参数,配置的参数大概有以下几类

<div class="counter">

* Parallelism 默认并行度配置
* TimeCharacteristic 时间特征配置
* checkpoint 检查点的相关配置
* Watermark 相关配置
* State Backend 状态后端配置
* Restart Strategy 重启策略配置
* 其他配置...

</div>

以上的配置基本都是比较普遍且通用的,是每个程序上来第一步就要定义的,是一项重复的工作

当程序写好后,要上线运行,任务启动提交都差不多用下面的命令行的方式,设置各种启动参数,
这时就得开发者清楚的知道每个参数的含义,如果再设置几个运行时资源参数,那启动命名会很长,可读性很差,参数解析用到了强校验,一旦设置错误,会直接报错,导致任务启动失败,最直接的异常是 ==找不到程序的jar==
```bash 
flink run -m yarn-cluster -p 1 -c com.xx.Main job.jar
```


开发`Flink Sql`程序,也需要设置一系列环境参数,除此之外,如果要使用纯sql的方式开发,举一个最简单的例子,代码如下

```java
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableEnvironment;

public class JavaTableApp {

    public static void main(String[] args) {
        EnvironmentSettings bbSettings = EnvironmentSettings
        .newInstance()
        .useBlinkPlanner()
        .build();
        
        TableEnvironment bsTableEnv = TableEnvironment.create(bbSettings);

        String sourceDDL = "CREATE TABLE datagen (  " +
                " f_random INT,  " +
                " f_random_str STRING,  " +
                " ts AS localtimestamp,  " +
                " WATERMARK FOR ts AS ts  " +
                ") WITH (  " +
                " 'connector' = 'datagen',  " +
                " 'rows-per-second'='10',  " +
                " 'fields.f_random.min'='1',  " +
                " 'fields.f_random.max'='5',  " +
                " 'fields.f_random_str.length'='10'  " +
                ")";

        bsTableEnv.executeSql(sourceDDL);

        String sinkDDL = "CREATE TABLE print_table (" +
                " f_random int," +
                " c_val bigint, " +
                " wStart TIMESTAMP(3) " +
                ") WITH ('connector' = 'print') ";
        
        bsTableEnv.executeSql(sinkDDL);
    }

}
```

我们会看到除了设置EnvironmentSettings参数之外,剩下的几乎大段大段的代码都是在写`sql`,用java代码拼接各种sql,这种编码的方式,极不优雅,如果业务复杂,更是难以维护,而且会发现,整个编码的模式是统一的,
都是声明一段sql,然后调用`executeSql`方法

**我们的设想是**:能不能以一种更好的方式将这种重复的工作简单化,将`DataStream`和`Flink Sql`任务中的一些环境初始化相关的参数和启动相关参数简化,最好一行代码都不写,针对`Flink Sql`作业,也不想在代码里写大段的sql,能不能以一种更优雅的方式解决? 

**答案是肯定的**

针对参数设置的问题,在`StreamX`中提出统一程序配置的概念,把程序的一系列参数从开发到部署阶段按照特定的格式配置到`application.yml`里,抽象出
一个通用的配置模板,按照这种规定的格式将上述配置的各项参数在配置文件里定义出来,在程序启动的时候将这个项目配置传入到程序中即可完成环境的初始化工作,在任务启动的时候也会自动识别启动时的参数,于是就有了`配置文件`这一概念

针对Flink Sql作业在代码里写sql的问题,`StreamX`针对`Flink Sql`作业做了更高层级封装和抽象,开发者只需要将sql按照一定的规范要求定义到`sql.yaml`文件中,在程序启动时将该sql文件传入到主程序中,
就会自动按照要求加载执行sql,于是就有了`sql文件`的概念

## 相关术语

为了方便开发者理解和相互交流,我们把上面引出的,把程序的一系列参数从开发到部署阶段按照特定的格式配置到文件里,这个有特定作用的文件就是项目的 <strong> ==`配置文件`== </strong>

Flink Sql任务中将提取出来的sql放到`sql.yaml`中,这个有特定作用的文件就是项目的 <strong> ==`sql文件`== </strong>

## 配置文件

在StreamX中,`DataStream`作业和`Flink Sql`作业配置文件是通用的,换言之,这个配置文件既能定义`DataStream`的各项配置,也能定义`Flink Sql`的各项配置(Flink Sql作业中配置文件是可选的), 配置文件的格式必须是`yaml`格式, 必须得符合yaml的格式规范

下面我们来详细看看这个配置文件的各项配置都是如何进行配置的,有哪些注意事项

```yaml
flink:
  deployment:
    option:
      target: application
      detached:
      shutdownOnAttachedExit:
      jobmanager:
    property: #@see: https://ci.apache.org/projects/flink/flink-docs-release-1.12/deployment/config.html
      $internal.application.main: com.streamxhub.streamx.flink.quickstart.QuickStartApp
      yarn.application.name: Streamx QuickStart App
      yarn.application.queue: 
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
```
上面是关于`开发时`和`项目部署`时需要关注的环境相关的完整的配置,这些配置是在`flink`的namespace下进行配置的,主要分为2大类
* `deployment`下的配置是项目部署相关的配置(`即项目启动时的一系列资源相关的配置参数`)
* 其他是开发时需要关注的环境相关的配置

开发时需要关注的环境相关的配置有5项

<div class="counter">

* `checkpoints`
* `watermark`
* `state`
* `restart-strategy`
* `table`

</div>

### Deployment
deployment下放的是部署相关的参数和配置项,具体又分为两类
* `option`
* `property`
#### option
`option`下放的参数是flink run 下支持的参数,目前支持的参数如下
<ClientOnly>
  <table-data name="option"></table-data>
</ClientOnly>
`parallelism` (-p) 并行度不支持在option里配置,会在后面的property里配置
`class` (-c) 程序main不支持在option里配置,会在后面的property里配置
::: info 注意事项
option下的参数必须是 `完整参数名`
:::
#### property
`property`下放的参数是标准参数-D下的参数,可以分为两类
- 基础参数
- Memory参数
##### 基础参数
基础参数可以配置的选项非常之多,这里举例5个最基础的设置
<ClientOnly>
  <table-data name="property"></table-data>
</ClientOnly>
::: info 注意事项
`$internal.application.main` 和 `yarn.application.name` 这两个参数是必须的
:::
如您需要设置更多的参数,可参考[`这里`](https://ci.apache.org/projects/flink/flink-docs-release-1.12/deployment/config.html)
一定要将这些参数放到`property`下,并且参数名称要正确,`StreamX`会自动解析这些参数并生效
##### Memory参数
Memory相关的参数设置也非常之多,一般常见的配置如下

<ClientOnly>
  <table-data name="memory"></table-data>
</ClientOnly>

同样,如你想配置更多的内存相关的参数,请参考[`这里`](https://ci.apache.org/projects/flink/flink-docs-release-1.12/deployment/memory/mem_setup.html) 查看[`Flink Process Memory`](https://ci.apache.org/projects/flink/flink-docs-release-1.12/deployment/memory/mem_setup.html) , [`jobmanager`](https://ci.apache.org/projects/flink/flink-docs-release-1.12/deployment/memory/mem_setup_tm.html) 及 [`taskmanager`](https://ci.apache.org/projects/flink/flink-docs-release-1.12/deployment/memory/mem_setup_jobmanager.html)
相关的内存配置将这些参数放到`property`下,保证参数正确即可生效

##### 配置总内存
Flink JVM 进程的进程总内存（Total Process Memory）包含了由 Flink 应用使用的内存（Flink 总内存）以及由运行 Flink 的 JVM 使用的内存。 Flink 总内存（Total Flink Memory）包括 JVM 堆内存（Heap Memory）和堆外内存（Off-Heap Memory）。 其中堆外内存包括直接内存（Direct Memory）和本地内存（Native Memory）

<center>
<img src="http://assets.Streamxhub.com/process_mem_model.svg" width="340px"/>
</center>

配置 Flink 进程内存最简单的方法是指定以下两个配置项中的任意一个：

<ClientOnly>
<table-data name="totalMem"></table-data>
</ClientOnly>


::: danger 注意事项
不建议同时设置进程总内存和 Flink 总内存。 这可能会造成内存配置冲突，从而导致部署失败。 额外配置其他内存部分时，同样需要注意可能产生的配置冲突。
:::

### Checkpoints

Checkpoints 的配置比较简单,按照下面的方式进行配置即可

<ClientOnly>
  <table-data name="checkpoints"></table-data>
</ClientOnly>


### Watermark

`watermark` 配置只需要设置下Watermark的生成周期`interval`即可

### State

`state`是设置状态相关的配置
```yaml
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
  checkpoints.num-retained: 1
```
我们可以看到大体可以分为两类
* backend 相关的配置
* checkpoints 相关的配置

#### backend
很直观的,`backend`下是设置状态后端相关的配置,状态后台的配置遵照[`官网文档`](https://ci.apache.org/projects/flink/flink-docs-release-1.12/ops/state/state_backends.html)的配置规则,在这里支持以下配置

<ClientOnly>
  <table-data name="backend"></table-data>
</ClientOnly>

如果`backend`的保存类型为`rocksdb`,则可能要进一步设置`rocksdb`相关的配置,可以参考[`官网`](https://ci.apache.org/projects/flink/flink-docs-release-1.12/deployment/config.html#rocksdb-state-backend)来进行相关配置,
需要注意的是官网关于`rocksdb`的配置都是以`state.backend`为前缀,而当前的命名空间就是在`state.backend`下,注意要保证参数名正确

::: info 注意事项
`value`项非标准配置,该项用来设置状态的保存类型(`jobmanager` | `filesystem` | `rocksdb`),其他项均为标准配置,遵守官网的规范
:::


### Restart Strategy
重启策略的配置非常直观,在flink中有三种重启策略,对应了这里的三种配置,如下:

```yaml
  restart-strategy:
    value: fixed-delay  #重启策略[(fixed-delay|failure-rate|none)共3个可配置的策略]
    fixed-delay:
      attempts: 3
      delay: 5000
    failure-rate:
      max-failures-per-interval:
      failure-rate-interval:
      delay:
```
`value`下配置具体的选择哪种重启策略

<div class="counter">

* fixed-delay
* failure-rate
* none

</div>

#### fixed-delay(固定间隔)
<ClientOnly>
<table-data name="fixed-delay"></table-data>
</ClientOnly>

::: tip 示例

```yaml
attempts: 5
delay: 3 s
```
即:任务最大的失败重试次数是`5次`,每次任务重启的时间间隔是`3秒`,如果失败次数到达5次,则任务失败退出
:::

#### failure-rate(失败率)
<ClientOnly>
<table-data name="failure-rate"></table-data>
</ClientOnly>

::: tip 示例

```yaml
 max-failures-per-interval: 10
 failure-rate-interval: 5 min
 delay: 2 s
```

即:每次异常重启的时间间隔是`2秒`,如果在`5分钟内`,失败总次数到达`10次` 则任务失败.
:::

#### None (无重启)

无重启无需配置任务参数

#### 单位后缀

时间间隔和频率设置需注意,可以不带单位后缀,如果不带单位后缀则默认会当成`毫秒`来处理,可选的单位有

* s    秒
* m    分钟
* min  分钟
* h    小时
* d    日

### Table
在`table`下是Flink Sql相关的配置,目前支持的配置项和作用如下

* planner
* mode
* catalog
* database

<ClientOnly>
  <table-data name="tables"></table-data>
</ClientOnly>


## Sql 文件

Sql 文件必须是yaml格式的文件,得遵循yaml文件的定义规则,具体内部sql格式的定义非常简单,如下:

```sql 
sql: |
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
`sql`为当前sql的id,必须是唯一的,后面的内容则是具体的sql

::: danger 特别注意

上面内容中 **sql:** 后面的 **|** 是必带的, 加上 **|** 会保留整段内容的格式,重点是保留了换行符, StreamX封装了Flink Sql的提交,可以直接将多个Sql一次性定义出来,每个Sql必须用 **;** 分割,每段 Sql也必须遵循Flink Sql规定的格式和规范
:::

## 总结

本章节详细介绍了`配置文件`和`sql文件`的由来和具体配置,相信你已经有了一个初步的印象和概念,具体使用请查后续章节


