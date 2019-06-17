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

package com.streamxhub.spark.monitor.domain

import java.util.Date
import scala.beans.BeanProperty
import javax.persistence.Id
import javax.persistence.GeneratedValue
import javax.persistence.Entity
import javax.persistence.Table
import java.lang.Long
import org.springframework.format.annotation.DateTimeFormat
import javax.validation.constraints.NotNull

@Table(name = "users")
@Entity
class User {

  @Id
  @GeneratedValue
  @BeanProperty
  var id: Long = _

  @BeanProperty
  @NotNull
  var name: String = _

  @BeanProperty
  @NotNull
  @DateTimeFormat(pattern = "yyyy-MM-dd")
  var birthday: Date = _

  @BeanProperty
  @NotNull
  var telephone: String = _

  override def toString = s"User($id, $name, $birthday, $telephone)"
}
