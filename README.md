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

**[Official Website](http://www.streamxhub.com)** |
**[Change Log](#)** |
**[Document](http://www.streamxhub.com/zh/doc/)**

</div>

English | [中文](README_CN.md)

# StreamX

Make stream processing easier

> A magical framework that make stream processing easier!

## 🚀 Introduction

The original intention of `StreamX` is to make stream processing easier. `StreamX` focuses on the management of development phases
and tasks. Our ultimate goal is to build a one-stop big data solution integrating stream processing, batch processing, data warehouse and
data laker.

[![StreamX video](https://user-images.githubusercontent.com/13284744/142747056-d220d69b-7f2a-447d-aeca-bc5435c8e29b.png)](http://assets.streamxhub.com/streamx-video.mp4)


![](https://user-images.githubusercontent.com/13284744/142746797-85ebf7b4-4105-4b5b-a023-0689c7fd1d2d.png)

## 🎉 Features

* Scaffolding
* Out-of-the-box connectors
* Support maven compilation
* Configuration
* Multi version flink support(1.12.x,1.13.x,1.14.x)
* on Kubernetes deployment (`K8s-Native-Application`/`K8s-Native-Session`)
* on YARN deployment (`YARN-Application`/`YARN-Per-Job`)
* Support `Applicaion` and `Yarn-Per-Job` mode
* `start`, `stop`, `savepoint`, resume from `savepoint`
* Flame graph
* Notebook
* Project configuration and dependency version management
* Task backup and rollback
* Manage dependencies
* UDF
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
respect, special thanks to [Apache Zeppelin](http://zeppelin.apache.org)
, [IntelliJ IDEA](https://www.jetbrains.com/idea/), Thanks to the [fire-spark](https://github.com/GuoNingNing/fire-spark) project for the
early inspiration and help.

### 🚀 Quick Start

```
git clone https://github.com/streamxhub/streamx.git
cd streamx
./mvnw clean install -DskipTests -Denv=prod
```

click [Document](http://www.streamxhub.com/zh/doc/) for more information

## 👻 Why not...❓

### Apache Zeppelin

[Apache Zeppelin](https://zeppelin.apache.org) is a Web-based notebook that enables data-driven, interactive data analytics and
collaborative documents with SQL, Java, Scala and more.

At the same time we also need a one-stop tool that can cover `development`, `test`, `package`, `deploy`, and `start`.
`streamx-console` solves these pain points very well, positioning is a one-stop stream processing platform, and has developed more exciting
features (such as `Flink SQL WebIDE`, `dependency isolation`, `task rollback `, `flame diagram`
etc.)

### FlinkX

[FlinkX](http://github.com/DTStack/flinkx) is a distributed offline and real-time data synchronization framework based on flink widely used
in DTStack, which realizes efficient data migration between multiple heterogeneous data sources.

`StreamX` focuses on the management of development phases and tasks. The `streamx-pump` module is also under planning, dedicated to solving
data source migration, and will eventually be integrated into the `streamx-console`.


## 🤝 Contributing

[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](https://github.com/streamxhub/streamx/pulls)

You can submit any ideas as [pull requests](https://github.com/streamxhub/streamx/pulls) or as [GitHub issues](https://github.com/streamxhub/streamx/issues/new/choose).

> If you're new to posting issues, we ask that you read [*How To Ask Questions The Smart Way*](http://www.catb.org/~esr/faqs/smart-questions.html) (**This guide does not provide actual support services for this project!**), [How to Report Bugs Effectively](http://www.chiark.greenend.org.uk/~sgtatham/bugs.html) prior to posting. Well written bug reports help us help you!

Thanks to [JetBrains](https://www.jetbrains.com/?from=streamx) for supporting us free open source licenses.

[![JetBrains](https://img.alicdn.com/tfs/TB1sSomo.z1gK0jSZLeXXb9kVXa-120-130.svg)](https://www.jetbrains.com/?from=streamx)


## 💰 Donation

Are you **enjoying this project** ? 👋

If you like this framework, and appreciate the work done for it to exist, you can still support the developers by donating ☀️ 👊

| WeChat Pay | Alipay |
|:----------|:----------|
| <img src="https://user-images.githubusercontent.com/13284744/142746857-35e7f823-7160-4505-be3f-e748a2d0a233.png" alt="Buy Me A Coffee" width="150"> | <img src="https://user-images.githubusercontent.com/13284744/142746860-e14a8183-d973-44ca-83bf-e5f9d4da1510.png" alt="Buy Me A Coffee" width="150"> |

## 🏆 My sponsors (Coffee Suppliers)

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

### 🏅 Backers

Thank you to all our backers!

## 💬 Join us

[StreamX]((http://www.streamxhub.com/#/)) enters the high-speed development stage, we need your contribution.


<div align="center">

![Stargazers over time](https://starchart.cc/streamxhub/streamx.svg)

</div>

<div align="center">
    <img src="https://user-images.githubusercontent.com/13284744/142746862-42e9e2d8-b574-4253-8aea-bf4d96c56088.jpeg" alt="Join the Group" height="250px"><br>
</div>


