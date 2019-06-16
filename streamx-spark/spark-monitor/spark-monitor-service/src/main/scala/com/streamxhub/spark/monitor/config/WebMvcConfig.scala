package com.streamxhub.spark.monitor.config

import java.util

import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration class WebMvcConfig extends WebMvcConfigurer {

  override def configureMessageConverters(converters: util.List[HttpMessageConverter[_]]): Unit = super.configureMessageConverters(converters)
}
