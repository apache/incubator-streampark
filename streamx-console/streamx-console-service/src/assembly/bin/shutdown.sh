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

# Get standard environment variables
# shellcheck disable=SC2006
PRG_DIR=`dirname "$PRG"`

# shellcheck disable=SC2006
# shellcheck disable=SC2164
APP_HOME=`cd "$PRG_DIR/.." >/dev/null; pwd`
APP_BASE="$APP_HOME"
APP_PID_DIR="/var/run";
if [[ ! -d "$APP_PID_DIR" ]] ; then
    mkdir ${APP_PID_DIR};
fi

APP_PID="$APP_BASE/streamx.pid"

pid=$(cat "${APP_PID}")

if [[ -z "${pid}" ]] ; then
  echo "StreamX already stopped."
else
  echo "StreamX pid is ${pid},stopping..."
  kill "${pid}"
  echo "StreamX stop successful!"
fi
