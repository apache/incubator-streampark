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
