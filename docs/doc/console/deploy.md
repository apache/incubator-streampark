---
title: '安装部署'
sidebar: true
author: 'benjobs'
original: true
date: 2020/03/25
---


> streamx-console 是一个综合实时数据平台,低代码(Low Code),Flink Sql平台,可以较好的管理Flink任务,集成了项目编译、发布、参数配置、启动、savepoint,火焰图(flame graph),Flink SQL,监控等诸多功能于一体,大大简化了Flink任务的日常操作和维护,融合了诸多最佳实践。其最终目标是打造成一个实时数仓,流批一体的一站式大数据解决方案

streamx-console 提供了开箱即用的安装包,安装之前对环境有些要求,具体要求如下

## 环境要求

<ClientOnly>
  <table-data name="envs"></table-data>
</ClientOnly>

### Hadoop
目前StreamX对flink的任务发布,同时支持`on yarn`和`on k8s`两种模式,如果是`on yarn`,需要部署的集群安装并配置hadoop的相关环境变量,如你是基于CDH安装的hadoop环境,
相关环境变量可以参考如下配置:
```shell
export HADOOP_HOME=/opt/cloudera/parcels/CDH/lib/hadoop #hadoop安装目录
export HADOOP_CONF_DIR=/etc/hadoop/conf
export HIVE_HOME=$HADOOP_HOME/../hive
export HBASE_HOME=$HADOOP_HOME/../hbase
export HADOOP_HDFS_HOME=$HADOOP_HOME/../hadoop-hdfs
export HADOOP_MAPRED_HOME=$HADOOP_HOME/../hadoop-mapreduce
export HADOOP_YARN_HOME=$HADOOP_HOME/../hadoop-yarn
```

## 编译 & 安装

你可以选择手动编译安装也可以直接下载编译好的安装包,手动编译安装步骤如下

### 编译

* Maven 3.6+
* npm 7.11.2 (https://nodejs.org/en/)
* JDK 1.8+

```bash
git clone https://github.com/streamxhub/streamx.git
cd Streamx
mvn clean install -DskipTests -Denv=prod
```

安装完成之后就看到最终的工程文件,位于 `streamx/streamx-console/streamx-console-service/target/streamx-console-service-1.0.0-bin.tar.gz`,解包后安装目录如下


```textmate 
.
streamx-console-service-1.0.0
├── bin
│    ├── flame-graph
│    ├──   └── *.py                                             //火焰图相关功能脚本(内部使用,用户无需关注)
│    ├── startup.sh                                             //启动脚本  
│    ├── setclasspath.sh                                        //java环境变量相关的脚本(内部使用,用户无需关注)
│    ├── shutdown.sh                                            //停止脚本
│    ├── yaml.sh                                                //内部使用解析yaml参数的脚本(内部使用,用户无需关注)
├── conf                                                        
│    ├── application.yaml                                       //项目的配置文件(注意不要改动名称)
│    ├── application-prod.yml                                   //项目的配置文件(开发者部署需要改动的文件,注意不要改动名称)
│    ├── flink-application.template                             //flink配置模板(内部使用,用户无需关注)
│    ├── logback-spring.xml                                     //logback
│    └── ...
├── lib
│    └── *.jar                                                  //项目的jar包
├── plugins   
│    ├── streamx-jvm-profiler-1.0.0.jar                         //jvm-profiler,火焰图相关功能(内部使用,用户无需关注)
│    └── streamx-flink-sqlclient-1.0.0.jar                      //Flink SQl提交相关功能(内部使用,用户无需关注)
├── logs                                                        //程序log目录
├── temp                                                        //内部使用到的零时路径,不要删除
```


### 修改配置
                     
安装解包已完成,接下来准备数据相关的工作
* 新建数据库`streamx`
  确保在部署机可以连接的mysql里新建数据库`streamx`
* 修改连接信息
  进入到`conf`下,修改`conf/application-prod.yml`,找到datasource这一项,找到mysql的配置,修改成对应的信息即可,如下

```yaml
  datasource:
    dynamic:
      # 是否开启 SQL日志输出，生产环境建议关闭，有性能损耗
      p6spy: false
      hikari:
        connection-timeout: 30000
        max-lifetime: 1800000
        max-pool-size: 15
        min-idle: 5
        connection-test-query: select 1
        pool-name: HikariCP-DS-POOL
      # 配置默认数据源
      primary: primary
      datasource:
        # 数据源-1，名称为 primary
        primary:
          username: $user
          password: $password
          driver-class-name: com.mysql.cj.jdbc.Driver
          url: jdbc:mysql://$host:$port/streamx?useUnicode=true&characterEncoding=UTF-8&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=GMT%2B8
```

:::info 提示
特别提示: 安装过程中不需要手动做数据初始化,只需要设置好数据库信息即可,会自动完成建表和数据初始化等一些列操作
:::

### 启动

进入到`bin`下直接执行startup.sh即可启动项目,默认端口是 ==10000==,如果没啥意外则会启动成功

```bash
cd streamx-console-service-1.0.0/bin
bash startup.sh
```
相关的日志会输出到 ==streamx-console-service-1.0.0/logs/streamx.out== 里

打开浏览器 输入 <strong> ==http://$host:10000== </strong> 即可登录,登录界面如下

<img src="http://assets.streamxhub.com/1621785003798.jpg"/>

::: info 提示
默认密码: <strong> admin / streamx </strong>
:::

## 系统配置

进入系统之后,第一件要做的事情就是修改系统配置,在菜单/StreamX/Setting下,操作界面如下:

<img src="http://assets.streamxhub.com/streamx-settings.png"/>

主要配置项分为以下几类

<div class="counter">

* Flink Home
* Maven Home
* StreamX Env
* Email

</div>

### Flink Home
这里配置全局的Flink Home,此处是系统唯一指定Flink环境的地方,会作用于所有的作业

::: info 提示
特别提示: 最低支持的Flink版本为 1.11.1, 之后的版本都支持
:::

### Maven Home

指定 maven Home, 目前暂不支持,下个版本实现

### StreamX Env

* StreamX Webapp address <br>
  这里配置StreamX Console的web url访问地址,主要火焰图功能会用到,具体任务会将收集到的信息通过此处暴露的url发送http请求到系统,进行收集展示<br>
* StreamX Console Workspace <br>
  配置系统的工作空间,用于存放项目源码,编译后的项目等

### Email

Alert Email相关的配置是配置发送者邮件的信息,具体配置请查阅相关邮箱资料和文档进行配置
