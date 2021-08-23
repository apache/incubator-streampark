/*
 * Copyright (c) 2021 The StreamX Project
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
package com.streamxhub.streamx.flink.k8s.enums

/**
 * K8s Event Action Enum
 */
object K8sEventAction extends Enumeration {

  val UNKNOWN: K8sEventAction.Value = Value("UNKNOWN")
  val ADD: K8sEventAction.Value = Value("ADD")
  val MODIFIED: K8sEventAction.Value = Value("MODIFIED")
  val DELETE: K8sEventAction.Value = Value("DELETE")

  def exists(action: String): Boolean = this.values.exists(_.toString == action)

  def of(action: String): Value = this.values.find(_.toString == action).getOrElse(UNKNOWN)

}
