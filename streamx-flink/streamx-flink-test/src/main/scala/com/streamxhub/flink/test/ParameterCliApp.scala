package com.streamxhub.flink.test

import com.streamxhub.common.conf.ParameterCli

object ParameterCliApp extends App {

    /**
     * 测试从yaml中解析参数.....
     */
  val array = "--detached /Users/benjobs/Github/StreamX/streamx-flink/streamx-flink-test/assembly/conf/application.yml -s hdfs://nameservice1/flink/savepoints/savepoint-142cb6-cc4258164dd4"
  ParameterCli.main(array.split("\\s+"))

}
