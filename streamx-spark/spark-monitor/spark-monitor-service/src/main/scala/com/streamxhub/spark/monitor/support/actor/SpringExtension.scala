package com.streamxhub.spark.monitor.support.actor

import akka.actor.{Extension, Props}
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component


@Component class SpringExtension extends Extension {

  private var applicationContext: ApplicationContext = _

  private[actor] def initialize(applicationContext: ApplicationContext): Unit = {
    this.applicationContext = applicationContext
  }

  def props(actorBeanName: String): Props = Props.create(classOf[SpringActorProducer], applicationContext, actorBeanName)
}
