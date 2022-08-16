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

Apache Flink and Apache Spark are widely used as the next generation of big data streaming computing engines.  Based on a bench of excellent experiences combined with best practices, we extracted the task deployment and runtime parameters into the configuration files. In this way,  an easy-to-use RuntimeContext with out-of-the-box connectors would bring easier and more efficient task development experience. It reduces the learning cost and development barriers, hence developers can focus on the business logic.
On the other hand, It can be challenge for enterprises to use Flink & Spark if there is no professional management platform for Flink & Spark tasks during the deployment phase. StreamX provides such a professional task management platform, including task development, scheduling, interactive query, deployment, operation, maintenance, etc.

[![StreamX video](https://user-images.githubusercontent.com/13284744/166101616-50a44d38-3ffb-4296-8a77-92f76a4c21b5.png)](http://assets.streamxhub.com/streamx-video.mp4)


## 🎉 Features

* Apache Flink & Spark application development scaffolding
* Out-of-the-box connectors
* Support maven compilation
* Support multiple versions of Flink & Spark
* Scala 2.11 / 2.12 support
* One-stop stream processing operation platform
* Support catalog、olap、streaming-warehouse etc.
* ...

![](https://user-images.githubusercontent.com/13284744/142746863-856ef1cd-fa0e-4010-b359-c16ca2ad2fb7.png)


![](https://user-images.githubusercontent.com/13284744/142746864-d807d728-423f-41c3-b90d-45ce2c21936b.png)


## 🏳‍🌈 Components

`Streamx` consists of three parts,`streamx-core`,`streamx-pump` and `streamx-console`

![](https://user-images.githubusercontent.com/13284744/142746859-f6a4dedc-ec42-4ed5-933b-c27d559b9988.png)

### 1️⃣ streamx-core

`streamx-core` is a framework that focuses on coding, standardizes configuration, and develops in a way that is better than configuration by
convention. Also it provides a development-time `RunTime Content` and a series of `Connector` out of the box. make application development easier
developer focus on the business itself, and improve development efficiency and development experience.

### 2️⃣ streamx-pump

`streamx-pump` is a planned data extraction component. Based on the various `connector` provided in `streamx-core`, the
purpose is to create a convenient, fast, out-of-the-box real-time data extraction and migration component for big data, and it will be
integrated into the `streamx-console`.

### 3️⃣ streamx-console

`streamx-console` is a stream processing and `Low Code` platform, capable of managing `Flink` & `Spark` tasks, integrating project compilation,
deploy, configuration, startup, `savepoint`, `flame graph`, monitoring and many other features. Simplify the daily operation
and maintenance of the `Flink` & `Spark` task.

Our ultimate goal is to build a one-stop big data solution integrating stream processing, batch processing, data warehouse.

* [Apache Flink](http://flink.apache.org)
* [Apache Spark](http://spark.apache.org)
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

Thanks for the respect given by the above excellent open source projects and many unmentioned excellent open source projects


## 🚀 Quick Start

click [Document](https://www.streamxhub.com/docs/intro) for more information

## 💋 our users

Various companies and organizations use StreamX for research, production and commercial products. Are you using this project ? [you can add your company](https://github.com/streamxhub/streamx/issues/163)

![image](https://user-images.githubusercontent.com/13284744/182794423-b77a09dd-ed45-4e87-a1bb-2a4646951f22.png)


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


## 💬 Join us

[StreamX]((http://www.streamxhub.com/#/)) enters the high-speed development stage, we need your contribution.


<div align="center">

![Stargazers over time](https://starchart.cc/streamxhub/streamx.svg)

</div>

<div align="center">
    <img src="https://user-images.githubusercontent.com/13284744/152627523-de455a4d-97c7-46cd-815f-3328a3fe3663.png" alt="Join the Group" height="300px"><br>
</div>


