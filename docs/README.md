# StreamX

> 一个神奇的框架,让Flink,Spark开发变得简单

## 什么是StreamX?

Streamx是Flink & Spark 极速开发脚手架,简化了Flink,spark的开发,提供了一系列开箱即用的source和sink,并标准化了开发、测试、部署,监控,运维的整个过程,StreamX有两部分组成,streamx-core 和 streamx-console

### streamx-core

streamx-core (Streamx-flink-core|Streamx-spark-core) 是一个开发时的框架,借鉴了SpringBoot的思想,规范了配置文件的格式,按照约定优于配置。为开发者提供了一个开发时 RunTime Content,提供了一个开箱即用的Source和Sink,每一个API都经过了仔细的打磨，并扩展了相关的方法（仅scala）大大简化了flink的开发，提高了开发效率和开发体验

### streamx-console

streamx-console 是一个独立的平台，它补充了streamx-core。较好地管理了flink任务，集成了项目编译、发布、参数配置、启动、savepoint、监控和维护等功能，并且集成了火焰图(flame graph),大大简化了flink任务的操作和维护。该平台本身采用SpringBoot Vue Mybatis开发,提供了简单的租户和权限管理,代码风格充分遵守阿里的开发规范,结构也尽可能的清晰规范,可以作为大数据平台的开发基础，很方便地进行二次开发
![console dashboard](https://7.dusays.com/2021/01/17/3ff1ea3a9faf5.jpg)
![job flameGraph](https://7.dusays.com/2021/01/17/8556991280bdc.png)
