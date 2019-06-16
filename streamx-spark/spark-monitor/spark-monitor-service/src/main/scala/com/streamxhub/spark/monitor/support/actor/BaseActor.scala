/**
  * Copyright (c) 2019 The StreamX Project
  * <p>
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements. See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership. The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License. You may obtain a copy of the License at
  * <p>
  * http://www.apache.org/licenses/LICENSE-2.0
  * <p>
  * Unless required by applicable law or agreed to in writing,
  * software distributed under the License is distributed on an
  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  * KIND, either express or implied. See the License for the
  * specific language governing permissions and limitations
  * under the License.
  */

package com.streamxhub.spark.monitor.support.actor

import java.io.Closeable
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, PoisonPill}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.slf4j.{Logger, LoggerFactory}

import scala.util.{Failure, Success, Try}

/**
  * Created by benjobs on 2019/05/05.
  *
  * preStart：在actor实例化后执行，重启时不会执行。
  * postStop：在actor正常终止后执行，异常重启时不会执行。
  * preRestart：在actor异常重启前保存当前状态。
  * postRestart：在actor异常重启后恢复重启前保存的状态。当异常引起了重启，新actor的postRestart方法被触发，默认情况下preStart方法被调用。
  *
  */
trait BaseActor extends Actor {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  implicit val timeout: Timeout = Timeout(30, TimeUnit.SECONDS)
  val config = ConfigFactory.load()


  override def preStart(): Unit = {
    logger.debug(s"Actor 启动 ${self.path} ...")
  }

  override def postStop(): Unit = {
    logger.debug(s"Actor 停止 ${self.path} ...")
  }

  override def postRestart(reason: Throwable): Unit = {
    logger.warn(s"Actor 重启 ${self.path} ${reason.getMessage}")
  }


  def close[T <: Closeable, R](f: T => R)(implicit cable: T): R = {
    val result = f(cable)
    Try {
      cable.close()
    } match {
      case Success(_) => logger.debug("cable.close successful.")
      case Failure(o) => logger.error(s"cable.close failed. ${o.getMessage}")
    }
    result
  }

  def withPoisonPill[R](f: ActorRef => R)(actor: ActorRef): R = {
    val result = f(actor)
    actor ! PoisonPill
    result
  }

}
