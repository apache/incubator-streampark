package com.streamxhub.spark.monitor.core.actor

import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import akka.actor.Extension
import akka.actor.Props


@Component class SpringExtension extends Extension {

  private var applicationContext: ApplicationContext = _

  private[actor] def initialize(applicationContext: ApplicationContext): Unit = {
    this.applicationContext = applicationContext
  }

  def props(actorBeanName: String): Props = Props.create(classOf[SpringActorProducer], applicationContext, actorBeanName)
}