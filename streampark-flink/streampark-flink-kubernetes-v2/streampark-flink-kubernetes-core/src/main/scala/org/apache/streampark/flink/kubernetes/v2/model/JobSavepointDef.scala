/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.streampark.flink.kubernetes.v2.model

/**
 * Flink Job savepoint definition.
 *
 * @param drain         If true, drain the job before taking savepoint.
 * @param savepointPath The path to save the savepoint.
 * @param formatType    The format type of the savepoint.
 * @param triggerId     The trigger id of the savepoint.
 */
case class JobSavepointDef(
    drain: Boolean = false,
    savepointPath: Option[String] = None,
    formatType: Option[String] = None,
    triggerId: Option[String] = None)

object JobSavepointDef {
  val CANONICAL_FORMAT = "CANONICAL"
  val NATIVE_FORMAT    = "NATIVE"
}
