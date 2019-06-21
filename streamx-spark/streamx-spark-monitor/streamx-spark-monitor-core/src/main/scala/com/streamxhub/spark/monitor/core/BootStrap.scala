package com.streamxhub.spark.monitor.core

import org.springframework.context.annotation.Configuration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.ComponentScan
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.autoconfigure.SpringBootApplication

@Configuration
@EnableAutoConfiguration
@ComponentScan
@SpringBootApplication
class BootStrap

object BootStrap extends App {
  new SpringApplicationBuilder(classOf[BootStrap]).run(args: _*)
}