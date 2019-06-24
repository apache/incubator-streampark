package com.streamxhub.spark.monitor.core.actor

import org.springframework.context.ApplicationContext
import akka.actor.Actor
import akka.actor.IndirectActorProducer


class SpringActorProducer(val applicationContext: ApplicationContext, val actorBeanName: String) extends IndirectActorProducer {

  override def produce: Actor = applicationContext.getBean(actorBeanName).asInstanceOf[Actor]

  override def actorClass: Class[_ <: Actor] = applicationContext.getType(actorBeanName).asInstanceOf[Class[_ <: Actor]]

}