#!/bin/bash
#
# Copyright (c) 2019 The StreamX Project
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements. See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership. The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License. You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied. See the License for the
# specific language governing permissions and limitations
# under the License.

# resolve links - $0 may be a softlink
PRG="$0"

while [[ -h "$PRG" ]]; do
  # shellcheck disable=SC2006
  ls=`ls -ld "$PRG"`
  # shellcheck disable=SC2006
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    # shellcheck disable=SC2006
    PRG=`dirname "$PRG"`/"$link"
  fi
done

# Get standard environment variables
# shellcheck disable=SC2006
PRG_DIR=`dirname "$PRG"`

# shellcheck disable=SC2006
# shellcheck disable=SC2164
APP_HOME=`cd "$PRG_DIR/.." >/dev/null; pwd`
APP_PID="$APP_HOME"/.pid

if [ -f "$APP_PID" ]; then
  pid=$(cat "${APP_PID}")
else
  # shellcheck disable=SC2006
  # StreamX main
  MAIN="com.streamxhub.streamx.console.StreamXConsole"
  # shellcheck disable=SC2006
  pid=`jps -l|grep $MAIN|awk '{print $1}'`
fi

if [[ -z "${pid}" ]] ; then
  echo "StreamX already stopped."
else
  echo "StreamX pid is ${pid},now stopping..."
  kill "${pid}"
  if [ $? -eq 0 ] ; then
    echo "StreamX stop successful!"
  else
    echo "StreamX stop failed!"
  fi
fi
