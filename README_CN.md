<div align="center">
    <br/>
    <h1>
        <a href="http://www.streamxhub.com" target="_blank" rel="noopener noreferrer">
        <img width="600" src="https://user-images.githubusercontent.com/13284744/166133644-ed3cc4f5-aae5-45bc-bfbe-29c540612446.png" alt="StreamX logo">
        </a>
    </h1>
    <strong style="font-size: 1.5rem">Make stream processing easier!!!</strong>
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
**[使用文档](https://www.streamxhub.com/zh-CN/docs/intro)**

</div>

#### [English](README.md) | 中文

# StreamX

Make stream processing easier

> 一个神奇的框架，让流处理更简单

## 🚀 什么是StreamX

实时即未来, 在实时处理流域 `Apache Spark` 和 `Apache Flink` 是一个伟大的进步,尤其是 `Apache Flink` 被普遍认为是下一代大数据流计算引擎, 我们在使用 `Flink` & `Spark` 时发现从编程模型, 参数配置到项目部署, 运维管理都有很多可以抽象共用的地方, 
我们将一些好的经验固化下来并结合业内的最佳实践, 通过不断努力终于诞生了今天的框架 —— `StreamX`, 项目的初衷是 —— 让流处理更简单, 使用 `StreamX` 开发, 可以极大降低学习成本和开发门槛, 让开发者只用关心最核心的业务, `StreamX` 规范了项目的配置,
鼓励函数式编程, 定义了最佳的编程方式, 提供了一系列开箱即用的 `Connectors`, 标准化了配置、开发、测试、部署、监控、运维的整个过程, 提供了 Scala/Java 两套 api, 其最终目的是打造一个一站式大数据平台, 流批一体,湖仓一体的解决方案

[![StreamX video](https://user-images.githubusercontent.com/13284744/166101616-50a44d38-3ffb-4296-8a77-92f76a4c21b5.png)](http://assets.streamxhub.com/streamx-video.mp4)


## 🎉 Features

* 开发脚手架
* 一系列开箱即用的connectors
* 支持项目编译功能(maven 编译)
* 在线参数配置
* 多版本flink支持(1.12.x,1.13.x,1.14.x)
* Scala 2.11 / 2.12 支持
* restapi 支持
* 快捷的日常操作(任务`启动`、`停止`、`savepoint`，从`savepoint`恢复)
* Flink所有部署模式的支持(`Remote`/`K8s-Native-Application`/`K8s-Native-Session`/`YARN-Application`/`YARN-Per-Job`/`YARN-Session`)
* 快捷的日常操作(任务`启动`、`停止`、`savepoint`，从`savepoint`恢复)
* 稳定可靠,诸多公司和组织将 `StreamX` 用于生产和商业产品中
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

感谢以上优秀的开源项目和很多未提到的优秀开源项目，致以最崇高的敬意，感谢 [Apache Flink](http://flink.apache.org) 创造了一个了不起的项目! 感谢 [Apache Zeppelin](http://zeppelin.apache.org) 早期给予的灵感，感谢我老婆在项目开发时给予的支持，悉心照顾我的生活和日常，给予我足够的时间开发这个项目!

### 🚀 快速上手

请查看[官网文档](http://www.streamxhub.com/zh/doc/)了解更多信息


## 💋 谁在使用

诸多公司和组织将 StreamX 用于研究、生产和商业产品中, 如果您也在使用 ? 可以在[这里添加](https://github.com/streamxhub/streamx/issues/163)

![image](https://user-images.githubusercontent.com/13284744/160220085-11f1e011-e7a0-421f-9294-c14213c0bc22.png)



## 🏆 我们的荣誉


我们获得了一些珍贵的荣誉, 这份荣誉属于参加建设 StreamX 的每一位朋友, 谢谢大家!

![](https://user-images.githubusercontent.com/13284744/142746797-85ebf7b4-4105-4b5b-a023-0689c7fd1d2d.png)


![](https://user-images.githubusercontent.com/13284744/163530071-a5b6f334-9af5-439c-96c9-2bb9b4eec6a6.jpg)



## 🤝 如何贡献

[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](https://github.com/streamxhub/streamx/pulls)

如果你希望参与贡献 欢迎 [Pull Request](https://github.com/streamxhub/streamx/pulls)，或给我们 [报告 Bug](https://github.com/streamxhub/streamx/issues/new/choose)。

> 强烈推荐阅读 [《提问的智慧》](https://github.com/ryanhanwu/How-To-Ask-Questions-The-Smart-Way)(**本指南不提供此项目的实际支持服务！**)、[《如何有效地报告 Bug》](http://www.chiark.greenend.org.uk/%7Esgtatham/bugs-cn.html)、[《如何向开源项目提交无法解答的问题》](https://zhuanlan.zhihu.com/p/25795393)，更好的问题更容易获得帮助。

感谢所有向 StreamX 贡献的朋友!

<a href="https://github.com/streamxhub/streamx/graphs/contributors"><img src="https://opencollective.com/streamx/contributors.svg?width=890" /></a>

## 💰 我要捐助

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


感谢 [JetBrains](https://www.jetbrains.com/?from=streamx) 提供的免费开源 License 赞助

[![JetBrains](https://img.alicdn.com/tfs/TB1sSomo.z1gK0jSZLeXXb9kVXa-120-130.svg)](https://www.jetbrains.com/?from=streamx)


## 💬 加入社区

`Streamx` 已正式开源，现已经进入高速发展模式，如果您觉得还不错请在右上角点一下 `star`，帮忙转发，谢谢 🙏🙏🙏 大家的支持是开源最大动力，
你可以扫下面的二维码加入官方微信群，更多相关信息请访问[官网](http://www.streamxhub.com/#/)

<div align="center">

![Stargazers over time](https://starchart.cc/streamxhub/streamx.svg)

</div>

<div align="center">
    <img src="https://user-images.githubusercontent.com/13284744/152627523-de455a4d-97c7-46cd-815f-3328a3fe3663.png" alt="关注我们" height="300px"><br>
</div>

