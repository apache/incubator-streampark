# 项目配置
> StreamX借鉴了SpringBoot的思想,规范了配置文件的格式,按照约定优于配置的方式进行项目开发

## 概述

在用Flink DataStream开发的过程中,我们会发现大体流程都可以抽象为以下4步

* StreamExecutionEnvironment初始并配置
* Source接入数据
* Transformation逻辑处理
* Sink结果数据落地

![](http://assets.streamxhub.com/20210317134700.png)

`Source`, `Transformation`, `Sink`后续有专门的章节进行讲解,这里我们重点关注StreamExecutionEnvironment初始和配置,我们会在第一步初始化StreamExecutionEnvironment并配置各种参数,配置的参数大概有以下几类
* Parallelism 默认并行度配置
* TimeCharacteristic 时间特征配置
* checkpoint 检查点的相关配置
* Watermark 相关配置
* State Backend 状态后端配置
* Restart Strategy 重启策略配置
* 其他配置...

会发现以上的配置基本都是比较普遍且通用的,是每个程序上来第一步就要定义的,是一项重复的工作.我们的设想是:`能不能以一种更好的方式将这种重复的工作简单化,最好一行代码都不写`,答案是肯定的,
在StreamX里提供了一个通用的配置模板,按照规定的格式将上述配置的各项参数直接在配置文件里定义出来,在程序启动的时候将这个配置文件传入到程序中即可完成环境的初始化工作

## 配置

我们来看看这个约定的配置,看看各项配置都是如何进行配置的,有哪些注意事项

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
      yarn.application.name: StreamX QuickStart App
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
    checkpoints.num-retained: 1
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
上面是关于项目部署和开发时需要关注的环境相关的完整的配置,这些配置是在`flink`的namespace下进行配置的,分为2大类
* `deployment`下的配置是项目部署相关的配置
* 其他是开发时需要关注的环境相关的配置

开发时需要关注的环境相关的配置有5项
* `checkpoints`
* `watermark`
* `state`
* `restart-strategy`
* `table`

### Deployment 
deployment下放的是部署相关的参数和配置项,具体又分为两类
* option
* property

#### option
`option`下放的参数是flink run 下支持的参数,目前支持的参数如下
<table class="option table" style="width: 100%;display: inline-table">
    <thead>
        <tr>
            <td>简短参数名</td>
            <td>完整参数名(前缀"-")</td>
            <td>是否支持</td>
            <td>取值范围值或类型</td>
            <td>作用描述</td>
        </tr>
    </thead>
    <tbody>
        <tr v-for="(item,index) in option">
            <td>{{item.opt}}</td>
            <td>{{item.longOpt}}</td>
            <td>
                <span class="fa fa-check" v-if="!item.deprecated" style="color:green"></span>
                <span class="fa fa-close" v-else style="color: red"></span>
            </td>
            <td>{{item.value}}</td>
            <td>{{item.desc}}</td>
        </tr>
    </tbody>
</table>

`parallelism` (-p) 并行度不支持在option里配置,会在后面的property里配置

`class` (-c) 程序main不支持在option里配置,会在后面的property里配置

?> 注意: option下的参数必须是 `完整参数名`

#### property
`property`下放的参数是标准参数-D下的参数,可以分为两类
- 基础参数
- Memory参数

##### 基础参数

基础参数可以配置的选项非常之多,这里举例5个最基础的设置
<table class="property table" style="width: 100%;display: inline-table">
    <thead>
        <tr>
            <td>参数名称</td>
            <td>作用描述</td>
            <td>是否必须</td>
        </tr>
    </thead>
    <tbody>
        <tr v-for="(item,index) in property">
            <td>{{item.name}}</td>
            <td>{{item.desc}}</td>
            <td>
                <span class="fa fa-toggle-on" v-if="item.required" style="color:green" title="必须"></span>
                <span class="fa fa-toggle-off" v-else style="color: gray" title="可选"></span>
            </td>
        </tr>
    </tbody>
</table>

?> 注意: `$internal.application.main` 和 `yarn.application.name` 这两个参数是必须的

如您需要设置更多的参数,可参考[`这里`](https://ci.apache.org/projects/flink/flink-docs-release-1.12/deployment/config.html)
一定要将这些参数放到`property`下,并且参数名称要正确,StreamX会自动解析这些参数并生效

##### Memory参数

Memory相关的参数设置也非常之多,一般常见的配置如下

<table class="memory table" style="width: 100%;display: inline-table">
    <thead>
        <tr>
            <td>组成部分</td>
            <td>参数名称</td>
            <td>作用描述</td>
        </tr>
    </thead>
    <tbody>
        <tr v-for="(item,index) in memory">
            <td>{{item.group}}</td>
            <td>{{item.name}}</td>
            <td>{{item.desc}}</td>
        </tr>
    </tbody>
</table>

同样,如你想配置更多的内存相关的参数,请参考[`这里`](https://ci.apache.org/projects/flink/flink-docs-release-1.12/deployment/memory/mem_setup.html) 查看[`Flink Process Memory`](https://ci.apache.org/projects/flink/flink-docs-release-1.12/deployment/memory/mem_setup.html) , [`jobmanager`](https://ci.apache.org/projects/flink/flink-docs-release-1.12/deployment/memory/mem_setup_tm.html) 及 [`taskmanager`](https://ci.apache.org/projects/flink/flink-docs-release-1.12/deployment/memory/mem_setup_jobmanager.html)
相关的内存配置将这些参数放到`property`下,保证参数正确即可生效

##### 配置总内存
Flink JVM 进程的进程总内存（Total Process Memory）包含了由 Flink 应用使用的内存（Flink 总内存）以及由运行 Flink 的 JVM 使用的内存。 Flink 总内存（Total Flink Memory）包括 JVM 堆内存（Heap Memory）和堆外内存（Off-Heap Memory）。 其中堆外内存包括直接内存（Direct Memory）和本地内存（Native Memory）

<center>
<img src="http://assets.streamxhub.com/process_mem_model.svg" width="340px"/>
</center>

配置 Flink 进程内存最简单的方法是指定以下两个配置项中的任意一个：

<table class="totalMem table" style="width: 100%;display: inline-table">
    <thead>
        <tr>
            <td>配置项</td>
            <td>TaskManager 配置参数</td>
            <td>JobManager 配置参数  </td>
        </tr>
    </thead>
    <tbody>
        <tr v-for="(item,index) in totalMem">
            <td>{{item.group}}</td>
            <td>{{item.tm}}</td>
            <td>{{item.jm}}</td>
        </tr>
    </tbody>
</table>

?> 不建议同时设置进程总内存和 Flink 总内存。 这可能会造成内存配置冲突，从而导致部署失败。 额外配置其他内存部分时，同样需要注意可能产生的配置冲突。

### Checkpoints

Checkpoints 的配置比较简单,按照下面的方式进行配置即可

<table class="checkpoints table" style="width: 100%;display: inline-table">
    <thead>
        <tr>
            <td>配置项</td>
            <td>作用描述</td>
            <td>参数值或类型</td>
        </tr>
    </thead>
    <tbody>
        <tr v-for="(item,index) in checkpoints">
            <td>{{item.name}}</td>
            <td>{{item.desc}}</td>
            <td>{{item.value}}</td>
        </tr>
    </tbody>
</table>

    
### Watermark

`watermark` 配置只需要设置下Watermark的生成周期`interval`即可

### State

### Restart Strategy

### Table


<script>
new Vue({
    el: '.markdown-section',
    data() {
        return {
            option: [
                {opt: '-t',longOpt: 'target',desc : '部署方式(目前只支持yarn-per-job,application)',deprecated : false,value : ' yarn-per-job | application '},
                {opt: '-d',longOpt: 'detached',desc : '是否以detached模式启动',deprecated : false,value : "true | false"},
                {opt: '-n',longOpt: 'allowNonRestoredState',desc : '从savePoint恢复数据失败时是否允许跳过数据恢复这一步',deprecated : false,value : "true | false"},
                {opt: '-sae',longOpt: 'shutdownOnAttachedExit',desc : '如任务以"attached"模式启动,在任务停止时是否关闭集群',deprecated : false,value : "true | false"},
                {opt: '-m',longOpt: 'jobmanager',desc : 'JobManager的连接地址',deprecated : false,value : "yarn-cluster | 连接地址"},
                {opt: '-p',longOpt: 'parallelism',desc : '程序并行度',deprecated : true, value: 'int'},
                {opt: '-c',longOpt: 'class',desc : '程序的main方法的全名称',deprecated : true, value : 'String'},
            ],
            property: [
                {name: '$internal.application.main',desc : '程序的主类(main)的完整类名',required: true},
                {name: 'yarn.application.name',desc : '程序的名称(YARN中显示的任务名称)',required: true},
                {name: 'yarn.application.queue',desc : '在YARN中运行的队列名称',required: false},
                {name: 'taskmanager.numberOfTaskSlots',desc : 'taskmanager Slot的数量',required: false},
                {name: 'parallelism.default',desc : '程序的并行',required: false}
            ],
            memory: [
                {group: 'JVM 堆内存', name: 'jobmanager.memory.heap.size',desc : 'JobManager 的 JVM 堆内存'},
                {group: '堆外内存',name: 'jobmanager.memory.off-heap.size',desc : 'JobManager 的堆外内存（直接内存或本地内存）'},
                {group: 'JVM Metaspace',name: 'jobmanager.memory.jvm-metaspace.size',desc : 'Flink JVM 进程的 Metaspace'},
                {group: 'JVM Metaspace',name: 'jobmanager.memory.jvm-metaspace.size',desc : 'Flink JVM 进程的 Metaspace'},
                {group: 'JVM Metaspace',name: 'jobmanager.memory.jvm-overhead.min',desc : 'Flink JVM 进程的 Metaspace'},
                {group: 'JVM 开销',name: 'jobmanager.memory.jvm-metaspace.size',desc : '用于其他 JVM 开销的本地内存'},
                {group: 'JVM 开销',name: 'jobmanager.memory.jvm-overhead.max',desc : '用于其他 JVM 开销的本地内存'},
                {group: 'JVM 开销',name: 'jobmanager.memory.jvm-overhead.fraction',desc : '用于其他 JVM 开销的本地内存'},
                {group: '框架堆内存',name: 'taskmanager.memory.framework.heap.size',desc : '用于Flink 框架的 JVM 堆内存（进阶配置）'},
                {group: '任务堆内存',name: 'taskmanager.memory.task.heap.size',desc : '由Flink管理的用于排序,哈希表,缓存StateBackend的本地内存'},
                {group: '托管内存',name: 'taskmanager.memory.managed.size',desc : '用于其他 JVM 开销的本地内存'},
                {group: '托管内存',name: 'taskmanager.memory.managed.fraction',desc : '用于其他 JVM 开销的本地内存'},
                {group: '框架堆外内存',name: 'taskmanager.memory.framework.off-heap.size',desc : '用于 Flink 框架的堆外内存(直接内存或本地内存)（进阶配置)'},
                {group: '任务堆外内存',name: 'taskmanager.memory.task.off-heap.size',desc : '用于 Flink 应用的算子及用户代码的堆外内存（直接内存或本地内存'},
                {group: 'JVM Metaspace',name: 'taskmanager.memory.jvm-metaspace.size',desc : 'Flink JVM 进程的 Metaspace'}
            ],
            totalMem: [
                {group: 'Flink 总内存 ', tm: 'taskmanager.memory.flink.size',jm : 'jobmanager.memory.flink.size'},
                {group: '进程总内存',tm: 'taskmanager.memory.process.size',jm : 'jobmanager.memory.process.size'}
            ],
            checkpoints: [
                {name: 'enable', desc: '是否开启checkpoint',value : 'true | false'},
                {name: 'interval',desc: 'checkpoint的间隔周期',value : '毫秒'},
                {name: 'mode',desc: '语义',value : ' EXACTLY_ONCE | AT_LEAST_ONCE '},
                {name: 'timeout',desc: '超时时间',value : '毫秒'},
                {name: 'unaligned',desc: '是否非对齐',value : 'true | false'},
            ],
        }
    }
});

</script>

