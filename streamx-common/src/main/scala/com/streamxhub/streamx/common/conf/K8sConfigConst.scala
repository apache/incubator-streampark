/*
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
package com.streamxhub.streamx.common.conf

/**
 * Config for K8s cluster
 * author: Al-assad
 */
object K8sConfigConst {

  /**
   * docker image regoster address for remote k8s cluster
   */
  lazy val KEY_K8S_IMAGE_REGISTER_ADDRESS = "k8s.image.register.address"
  lazy val K8S_IMAGE_REGISTER_ADDRESS: String = System.getProperty(KEY_K8S_IMAGE_REGISTER_ADDRESS)

  /**
   * login username of docker image regoster for remote k8s cluster
   */
  lazy val KEY_K8S_IMAGE_REGISTER_USERNAME = "k8s.image.register.username"
  lazy val K8S_IMAGE_REGISTER_USERNAME:String = System.getProperty(KEY_K8S_IMAGE_REGISTER_ADDRESS)

  /**
   * login password of docker image regoster for remote k8s cluster
   */
  lazy val KEY_K8S_IMAGE_REGISTER_PASSWORD = "k8s.image.register.password"
  lazy val K8S_IMAGE_REGISTER_PASSWORD:String = System.getProperty(KEY_K8S_IMAGE_REGISTER_ADDRESS)



}
