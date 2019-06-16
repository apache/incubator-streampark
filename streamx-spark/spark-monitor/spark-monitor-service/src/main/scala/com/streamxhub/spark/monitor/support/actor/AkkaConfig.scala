package com.streamxhub.spark.monitor.support.actor

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.{Bean, Configuration}


@Configuration
class AkkaConfig @Autowired()(val applicationContext: ApplicationContext, val springExtension: SpringExtension) {

  @Bean def actorSystem: ActorSystem = {
    val actorSystem = ActorSystem.create("ActorSystem")
    springExtension.initialize(applicationContext)
    actorSystem
  }

  @Bean def akkaConfiguration: Config = ConfigFactory.load
}
