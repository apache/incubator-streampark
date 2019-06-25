package com.streamxhub.spark.monitor.core

import org.springframework.context.annotation.Configuration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.ComponentScan
import org.springframework.boot.autoconfigure.SpringBootApplication

import org.springframework.boot.SpringApplication

@Configuration
@EnableAutoConfiguration
@ComponentScan(value = Array("com.streamxhub.spark.monitor"))
@SpringBootApplication
class BootStrap

object BootStrap extends App {
  SpringApplication.run(classOf[BootStrap], args: _*)
}
