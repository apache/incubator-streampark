#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

-server
-Xms1g
-Xmx1g
-Xmn512m

-XX:+ExitOnOutOfMemoryError
-XX:+HeapDumpOnOutOfMemoryError
-XX:+IgnoreUnrecognizedVMOptions

-XX:+PrintGCDateStamps
-XX:+PrintGCDetails
-XX:+PrintGC

-XX:+UseGCLogFileRotation
-XX:GCLogFileSize=50M
-XX:NumberOfGCLogFiles=10

# solved jdk1.8+ dynamic loading of resources to the classpath issue, if jdk > 1.8, you can enable this parameter
#--add-opens java.base/jdk.internal.loader=ALL-UNNAMED --add-opens jdk.zipfs/jdk.nio.zipfs=ALL-UNNAMED
