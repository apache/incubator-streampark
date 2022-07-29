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
  <img src="https://tokei.rs/b1/github/streamxhub/streamx">
  <img src="https://img.shields.io/github/v/release/streamxhub/streamx.svg">
  <img src="https://img.shields.io/github/stars/streamxhub/streamx">
  <img src="https://img.shields.io/github/forks/streamxhub/streamx">
  <img src="https://img.shields.io/github/issues/streamxhub/streamx">
  <img src="https://img.shields.io/github/downloads/streamxhub/streamx/total.svg">
  <img src="https://img.shields.io/github/languages/count/streamxhub/streamx">
  <a href="https://www.apache.org/licenses/LICENSE-2.0.html"><img src="https://img.shields.io/badge/license-Apache%202-4EB1BA.svg"></a>
</p>

<div align="center">

**[Official Website](http://www.streamxhub.com)** |
**[Change Log](#)** |
**[Document](https://www.streamxhub.com/docs/intro)**

</div>

#### English | [中文](README_CN.md)

# StreamX

Make stream processing easier

> A magical framework that make stream processing easier!

## 🚀 Introduction

The original intention of `StreamX` is to make stream processing easier. `StreamX` focuses on the management of development phases
and tasks. Our ultimate goal is to build a one-stop big data solution integrating stream processing, batch processing, data warehouse and
data laker.

[![StreamX video](https://user-images.githubusercontent.com/13284744/166101616-50a44d38-3ffb-4296-8a77-92f76a4c21b5.png)](http://assets.streamxhub.com/streamx-video.mp4)


## 🎉 Features

* Scaffolding
* Out-of-the-box connectors
* Support maven compilation
* Configuration
* Multi version flink support(1.12.x,1.13.x,1.14.x, 1.15.x)
* Scala 2.11 / 2.12 support
* restapi support.
* All Flink deployment mode support(`Remote`/`K8s-Native-Application`/`K8s-Native-Session`/`YARN-Application`/`YARN-Per-Job`/`YARN-Session`)
* `start`, `stop`, `savepoint`, resume from `savepoint`
* Various companies and organizations use `StreamX` for production and commercial products.
* Flame graph
* Notebook
* Project configuration and dependency version management
* Task backup and rollback
* Manage dependencies
* UDF
* Flink SQL Connector
* Flink SQL WebIDE
* Catalog、Hive
* Full support from task `development` to `deployment`
* ...

![](https://user-images.githubusercontent.com/13284744/142746863-856ef1cd-fa0e-4010-b359-c16ca2ad2fb7.png)


![](https://user-images.githubusercontent.com/13284744/142746864-d807d728-423f-41c3-b90d-45ce2c21936b.png)



## 🏳‍🌈 Components

`Streamx` consists of three parts,`streamx-core`,`streamx-pump` and `streamx-console`

![](https://user-images.githubusercontent.com/13284744/142746859-f6a4dedc-ec42-4ed5-933b-c27d559b9988.png)

### 1️⃣ streamx-core

`streamx-core` is a framework that focuses on coding, standardizes configuration, and develops in a way that is better than configuration by
convention. Also it provides a development-time `RunTime Content` and a series of `Connector` out of the box. At the same time, it
extends `DataStream` some methods, and integrates `DataStream` and `Flink sql` api to simplify tedious operations, focus on the business
itself, and improve development efficiency and development experience.

### 2️⃣ streamx-pump

`streamx-pump` is a planned data extraction component, similar to `flinkx`. Based on the various `connector` provided in `streamx-core`, the
purpose is to create a convenient, fast, out-of-the-box real-time data extraction and migration component for big data, and it will be
integrated into the `streamx-console`.

### 3️⃣ streamx-console

`streamx-console` is a stream processing and `Low Code` platform, capable of managing `Flink` tasks, integrating project compilation,
deploy, configuration, startup, `savepoint`, `flame graph`, `Flink SQL`, monitoring and many other features. Simplify the daily operation
and maintenance of the `Flink` task.

Our ultimate goal is to build a one-stop big data solution integrating stream processing, batch processing, data warehouse and data laker.

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

Thanks to the above excellent open source projects and many outstanding open source projects that are not mentioned, for giving the greatest
respect,Thanks to [Apache Flink](http://flink.apache.org) for creating a great project!  Thanks to the [Apache Zeppelin](http://zeppelin.apache.org) project for the early inspiration.

### 🚀 Quick Start

click [Document](https://www.streamxhub.com/docs/intro) for more information


## 💋 our users


Various companies and organizations use StreamX for research, production and commercial products. Are you using this project ? [you can add your company](https://github.com/streamxhub/streamx/issues/163)

![image](https://user-images.githubusercontent.com/13284744/160220085-11f1e011-e7a0-421f-9294-c14213c0bc22.png)



## 🏆 Our honor

We have received some precious honors, which belong to everyone who contributes to StreamX, Thank you !


![](https://user-images.githubusercontent.com/13284744/142746797-85ebf7b4-4105-4b5b-a023-0689c7fd1d2d.png)

![](https://user-images.githubusercontent.com/13284744/174478150-78e078b2-739f-49a3-8d49-d4763a01268f.jpg)


## 🤝 Contribution

[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](https://github.com/streamxhub/streamx/pulls)

You can submit any ideas as [pull requests](https://github.com/streamxhub/streamx/pulls) or as [GitHub issues](https://github.com/streamxhub/streamx/issues/new/choose).

> If you're new to posting issues, we ask that you read [*How To Ask Questions The Smart Way*](http://www.catb.org/~esr/faqs/smart-questions.html) (**This guide does not provide actual support services for this project!**), [How to Report Bugs Effectively](http://www.chiark.greenend.org.uk/~sgtatham/bugs.html) prior to posting. Well written bug reports help us help you!

Thank you to all the people who already contributed to StreamX!

<a href="https://github.com/streamxhub/streamx/graphs/contributors">
    <img src="https://contrib.rocks/image?repo=streamxhub/streamx" />
</a>


## ⏰ Contributor Over Time

[![Contributor Over Time](https://contributor-overtime-api.git-contributor.com/contributors-svg?chart=contributorOverTime&repo=streamxhub/streamx)](https://git-contributor.com?chart=contributorOverTime&repo=streamxhub/streamx)


## 💰 Donation

Are you **enjoying this project** ? 👋

If you like this framework, and appreciate the work done for it to exist, you can still support the developers by donating ☀️ 👊

| WeChat Pay | Alipay |
|:----------|:----------|
| <img src="https://user-images.githubusercontent.com/13284744/142746857-35e7f823-7160-4505-be3f-e748a2d0a233.png" alt="Buy Me A Coffee" width="150"> | <img src="https://user-images.githubusercontent.com/13284744/142746860-e14a8183-d973-44ca-83bf-e5f9d4da1510.png" alt="Buy Me A Coffee" width="150"> |

## 🏆 Our sponsors (Coffee Suppliers)

### 💜 Monthly Supplier

Welcome individuals and enterprises to sponsor, your support will help us better develop the project

### 🥇 Gold Supplier

<p>
  <a href="https://github.com/wolfboys" alt="benjobs"><img src="https://avatars.githubusercontent.com/u/13284744?v=4" height="50" width="50"></a>
  <a href="https://github.com/Kitming25" alt="Kitming25"><img src="https://avatars.githubusercontent.com/u/11773106?v=4" height="50" width="50"></a>
  <a href="https://github.com/Narcasserun" alt="Narcasserun"><img src="https://avatars.githubusercontent.com/u/39329477?v=4" height="50" width="50"></a>
</p>

### 🥈 Platinum Supplier

<p>
    <a href="https://github.com/lianxiaobao" alt="lianxiaobao"><img src="https://avatars.githubusercontent.com/u/36557317?v=4" height="50" width="50"></a>
    <a href="https://github.com/su94998" alt="su94998"><img src="https://avatars.githubusercontent.com/u/33316193?v=4" height="50" width="50"></a>
</p>

### 🥈 Silver Supplier

<p>
    <a href="https://github.com/CrazyJugger" alt="leohantaoluo"><img src="https://avatars.githubusercontent.com/u/30514978?v=4" height="50" width="50"></a>
    <a href="https://github.com/zhaizhirui" alt="zhaizhirui"><img src="https://avatars.githubusercontent.com/u/39609947?v=4" height="50" width="50"></a>
</p>


Thanks to [JetBrains](https://www.jetbrains.com/?from=streamx) for supporting us free open source licenses.

[![JetBrains](https://img.alicdn.com/tfs/TB1sSomo.z1gK0jSZLeXXb9kVXa-120-130.svg)](https://www.jetbrains.com/?from=streamx)


### 🏅 Backers

Thank you to all our backers!

---

## 💬 Join us

[StreamX]((http://www.streamxhub.com/#/)) enters the high-speed development stage, we need your contribution.


<div align="center">

![Stargazers over time](https://starchart.cc/streamxhub/streamx.svg)

</div>

<div align="center">
    <img src="https://user-images.githubusercontent.com/13284744/152627523-de455a4d-97c7-46cd-815f-3328a3fe3663.png" alt="Join the Group" height="300px"><br>
</div>


