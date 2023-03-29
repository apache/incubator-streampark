/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// flink home data
export interface FlinkEnv {
  id: string;
  flinkName: string;
  flinkHome: string;
  flinkConf: string;
  description: string;
  scalaVersion: string;
  version: string;
  isDefault: boolean;
  createTime: string;
  streamParkScalaVersion: string;
}

export interface FlinkCreate {
  id?: string | null;
  flinkName: string;
  flinkHome: string;
  description: string;
}
