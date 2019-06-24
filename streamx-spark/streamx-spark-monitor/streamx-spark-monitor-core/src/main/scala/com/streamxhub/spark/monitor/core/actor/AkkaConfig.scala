package com.streamxhub.spark.monitor.core.actor

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem


@Configuration class AkkaConfig @Autowired()(val applicationContext: ApplicationContext, val springExtension: SpringExtension) {

  @Bean def actorSystem: ActorSystem = {
    val actorSystem = ActorSystem.create("ActorSystem")
    springExtension.initialize(applicationContext)
    actorSystem
  }

  @Bean def akkaConfiguration: Config = ConfigFactory.load
}
