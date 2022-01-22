<div align="center">
    <br/>
    <h1>
        <a href="http://www.streamxhub.com" target="_blank" rel="noopener noreferrer">
        <img width="500" src="https://user-images.githubusercontent.com/13284744/142753483-4e96eb33-01ee-469d-ad7c-387e1bf95ee1.png" alt="StreamX logo">
        </a>
    </h1>
    <strong>Make stream processing easier!!!</strong>
</div>

<br/>

<p align="center">
  <a href="https://www.apache.org/licenses/LICENSE-2.0.html"><img src="https://img.shields.io/badge/license-Apache%202-4EB1BA.svg"></a>
  <img src="https://tokei.rs/b1/github/streamxhub/streamx">
  <img src="https://img.shields.io/github/v/release/streamxhub/streamx.svg">
  <img src="https://img.shields.io/github/stars/streamxhub/streamx">
  <img src="https://img.shields.io/github/forks/streamxhub/streamx">
  <img src="https://img.shields.io/github/languages/count/streamxhub/streamx">
</p>

<div align="center">

**[官网](http://www.streamxhub.com)** |
**[更新日志](#)** |
**[使用文档](http://www.streamxhub.com/zh/doc)**

</div>

##[English](README.md) | 中文

# StreamX

Make stream processing easier

> 一个神奇的框架，让流处理更简单

## 🚀 什么是StreamX

&nbsp;&nbsp;&nbsp;&nbsp;大数据技术如今发展的如火如荼，已经呈现百花齐放欣欣向荣的景象，实时处理流域 `Apache Spark` 和 `Apache Flink`
更是一个伟大的进步，尤其是 `Apache Flink` 被普遍认为是下一代大数据流计算引擎， 我们在使用 `Flink` 时发现从编程模型， 启动配置到运维管理都有很多可以抽象共用的地方， 我们将一些好的经验固化下来并结合业内的最佳实践， 通过不断努力终于诞生了今天的框架
—— `StreamX`， 项目的初衷是 —— 让 `Flink` 开发更简单， 使用 `StreamX` 开发，可以极大降低学习成本和开发门槛， 让开发者只用关心最核心的业务， `StreamX`
规范了项目的配置，鼓励函数式编程，定义了最佳的编程方式，提供了一系列开箱即用的 `Connectors` ，标准化了配置、开发、测试、部署、监控、运维的整个过程， 提供 `Scala` 和 `Java` 两套api，
其最终目的是打造一个一站式大数据平台，流批一体，湖仓一体的解决方案

[![StreamX video](https://user-images.githubusercontent.com/13284744/142747056-d220d69b-7f2a-447d-aeca-bc5435c8e29b.png)](http://assets.streamxhub.com/streamx-video.mp4)


![](https://user-images.githubusercontent.com/13284744/142746797-85ebf7b4-4105-4b5b-a023-0689c7fd1d2d.png)

## 🎉 Features

* 开发脚手架
* 一系列开箱即用的connectors
* 支持项目编译功能(maven 编译)
* 在线参数配置
* 多版本flink支持(1.12.x,1.13.x,1.14.x)
* on Kubernetes部署模式支持(`K8s-Native-Application`/`K8s-Native-Session`)
* on YARN部署模式支持(`YARN-Application`/`YARN-Per-Job`)
* 快捷的日常操作(任务`启动`、`停止`、`savepoint`，从`savepoint`恢复)
* 支持火焰图
* 支持 `notebook` (在线任务开发)
* 项目配置和依赖版本化管理
* 支持任务备份、回滚(配置回滚)
* 在线管理依赖(maven pom)和自定义jar
* 自定义 udf、连接器等支持
* Flink SQL WebIDE
* 支持 Catalog、Hive
* 从任务`开发`阶段到`部署管理`全链路支持
* ...

![](https://user-images.githubusercontent.com/13284744/142746863-856ef1cd-fa0e-4010-b359-c16ca2ad2fb7.png)

![](https://user-images.githubusercontent.com/13284744/142746864-d807d728-423f-41c3-b90d-45ce2c21936b.png)

## 🏳‍🌈 组成部分

`Streamx` 由三部分组成，分别是 `streamx-core`，`streamx-pump` 和 `streamx-console`

![](https://user-images.githubusercontent.com/13284744/142746859-f6a4dedc-ec42-4ed5-933b-c27d559b9988.png)

### 1️⃣ streamx-core

`streamx-core` 定位是一个开发时框架，关注编码开发，规范了配置文件，按照约定优于配置的方式进行开发，提供了一个开发时 `RunTime Content` 和一系列开箱即用的 `Connector`
，扩展了 `DataStream` 相关的方法，融合了`DataStream` 和 `Flink sql` api，简化繁琐的操作，聚焦业务本身，提高开发效率和开发体验

### 2️⃣ streamx-pump

`pump` 是抽水机，水泵的意思，`streamx-pump` 的定位是一个数据抽取的组件，类似于 `flinkx`，基于`streamx-core` 中提供的各种 `connector`
开发，目的是打造一个方便快捷，开箱即用的大数据实时数据抽取和迁移组件，并且集成到 `streamx-console` 中，解决实时数据源获取问题，目前在规划中

### 3️⃣ streamx-console

`streamx-console` 是一个综合实时数据平台，低代码(`Low Code`)平台，可以较好的管理`Flink`任务，集成了项目编译、发布、参数配置、启动、`savepoint`，火焰图(`flame graph`)
，`Flink SQL`， 监控等诸多功能于一体，大大简化了 `Flink` 任务的日常操作和维护，融合了诸多最佳实践。旧时王谢堂前燕，飞入寻常百姓家，让大公司有能力研发使用的项目，现在人人可以使用，
其最终目标是打造成一个实时数仓，流批一体的一站式大数据解决方案，该平台使用但不仅限以下技术:

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

感谢以上优秀的开源项目和很多未提到的优秀开源项目，致以最崇高的敬意，特别感谢[Apache Zeppelin](http://zeppelin.apache.org)
，[IntelliJ IDEA](https://www.jetbrains.com/idea/)，
感谢 [fire-spark](https://github.com/GuoNingNing/fire-spark) 项目，早期给予的灵感和帮助， 感谢我老婆在项目开发时给予的支持，悉心照顾我的生活和日常，给予我足够的时间开发这个项目

### 🚀 快速上手

```
git clone https://github.com/streamxhub/streamx.git
cd streamx
mvn clean install -DskipTests -Denv=prod
```

更多请查看[官网文档](http://www.streamxhub.com/zh/doc/)

## 👻 为什么不是...❓

### Apache Zeppelin

[Apache Zeppelin](http://zeppelin.apache.org)是一个非常优秀的开源项目👏 对 `Flink` 做了很好的支持，`Zeppelin` 创新型的 `notebook`
功能，让开发者非常方便的 `On-line` 编程，快捷的提交任务，语言层面同时支持`Java`，`Scala`，`Python`，国内阿里的章剑峰大佬也在积极推动该项目，向剑峰大佬致以崇高的敬意🙏🙏🙏，
但该项目目前貌似没有解决项目的管理和运维方面的痛点，针对比较复杂的项目和大量的作业管理就有些力不从心了，一般来讲不论是 `DataStream` 作业还是 `Flink SQL` 作业，大概都会经历作业的`开发阶段`，`测试阶段`
，`打包阶段`，`上传服务器阶段`，`启动任务阶段`等这些步骤，这是一个链路很长的步骤，且整个过程耗时比较长，体验不好，
即使修改了一个符号，项目改完上线都得走上面的流程，我们期望这些步骤能够动动鼠标一键式解决，还希望至少能有一个任务列表的功能，能够方便的管理任务，可以清楚的看到哪些任务正在运行，哪些停止了，任务的资源消耗情况，可以在任务列表页面一键`启动`
或`停止`任务，并且自动管理 `savePoint`，这些问题也是开发者实际开发中会遇到了问题，
`streamx-console` 很好的解决了这些痛点，定位是一个一站式实时数据平台，并且开发了更多令人激动的功能(诸如`Flink SQL WebIDE`，`依赖隔离`，`任务回滚`，`火焰图`等)

### FlinkX

[FlinkX](http://github.com/DTStack/flinkx)
是基于flink的分布式数据同步工具，实现了多种异构数据源之间高效的数据迁移，定位比较明确，专门用来做数据抽取和迁移，可以作为一个服务组件来使用，`StreamX` 关注开发阶段和任务后期的管理，定位有所不同，`streamx-pump`
模块也在规划中， 致力于解决数据源抽取和迁移，最终会集成到 `streamx-console` 中


## 🤝 如何贡献

[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](https://github.com/streamxhub/streamx/pulls)

如果你希望参与贡献 欢迎 [Pull Request](https://github.com/streamxhub/streamx/pulls)，或给我们 [报告 Bug](https://github.com/streamxhub/streamx/issues/new/choose)。

> 强烈推荐阅读 [《提问的智慧》](https://github.com/ryanhanwu/How-To-Ask-Questions-The-Smart-Way)(**本指南不提供此项目的实际支持服务！**)、[《如何有效地报告 Bug》](http://www.chiark.greenend.org.uk/%7Esgtatham/bugs-cn.html)、[《如何向开源项目提交无法解答的问题》](https://zhuanlan.zhihu.com/p/25795393)，更好的问题更容易获得帮助。

感谢 [JetBrains](https://www.jetbrains.com/?from=streamx) 提供的免费开源 License 赞助

[![JetBrains](https://img.alicdn.com/tfs/TB1sSomo.z1gK0jSZLeXXb9kVXa-120-130.svg)](https://www.jetbrains.com/?from=streamx)

## 💰 Donation

Are you **enjoying this project** ? 👋

StreamX 采用 Apache LICENSE 2.0 许可的开源项目，使用完全免费， 旧时王谢堂前燕，飞入寻常百姓家，让大公司有能力研发使用的项目，现在人人都可以使用，流批一体，湖仓一体，数据湖是大数据领域的趋势，
StreamX现在离这个目标还有一段距离，还需持续投入，相应的资金支持能更好的持续项目的维护和开发。你可以通过下列的方法来赞助 StreamX 的开发， 欢迎捐助，一起来帮我们做的更好! ☀️ 👊

| 微信支付 | 支付宝 |
|:----------|:----------|
| <img src="https://user-images.githubusercontent.com/13284744/142746857-35e7f823-7160-4505-be3f-e748a2d0a233.png" alt="Buy Me A Coffee" width="150"> | <img src="https://user-images.githubusercontent.com/13284744/142746860-e14a8183-d973-44ca-83bf-e5f9d4da1510.png" alt="Buy Me A Coffee" width="150"> |

> 所有赞助人将在赞助人名单中显示。

## 🏆 我的赞助

### 💜 月度赞助商

虚席以待,欢迎个人和企业前来赞助,您的支持会帮助我们更好的发展项目

### 🥇 金牌赞助商

<p>
  <a href="https://github.com/wolfboys" alt="benjobs"><img src="https://avatars.githubusercontent.com/u/13284744?v=4" height="50" width="50"></a>
  <a href="https://github.com/Kitming25" alt="Kitming25"><img src="https://avatars.githubusercontent.com/u/11773106?v=4" height="50" width="50"></a>
  <a href="https://github.com/Narcasserun" alt="Narcasserun"><img src="https://avatars.githubusercontent.com/u/39329477?v=4" height="50" width="50"></a>
</p>

### 🥈 白金赞助商

<p>
    <a href="https://github.com/lianxiaobao" alt="lianxiaobao"><img src="https://avatars.githubusercontent.com/u/36557317?v=4" height="50" width="50"></a>
    <a href="https://github.com/su94998" alt="su94998"><img src="https://avatars.githubusercontent.com/u/33316193?v=4" height="50" width="50"></a>
</p>

### 🥈 白银赞助商

<p>
    <a href="https://github.com/CrazyJugger" alt="leohantaoluo"><img src="https://avatars.githubusercontent.com/u/30514978?v=4" height="50" width="50"></a>
    <a href="https://github.com/zhaizhirui" alt="zhaizhirui"><img src="https://avatars.githubusercontent.com/u/39609947?v=4" height="50" width="50"></a>
</p>

## 💬 加入社区

`Streamx` 已正式开源，现已经进入高速发展模式，如果您觉得还不错请在右上角点一下 `star`，帮忙转发，谢谢 🙏🙏🙏 大家的支持是开源最大动力，
你可以扫下面的二维码加入官方微信群，更多相关信息请访问[官网](http://www.streamxhub.com/#/)

<div align="center">

![Stargazers over time](https://starchart.cc/streamxhub/streamx.svg)

</div>

<div align="center">
    <img src="https://user-images.githubusercontent.com/13284744/142746862-42e9e2d8-b574-4253-8aea-bf4d96c56088.jpeg" alt="关注我们" height="250px"><br>
</div>

