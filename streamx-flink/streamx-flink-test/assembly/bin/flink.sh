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
#
# -----------------------------------------------------------------------------
#Start Script for the StreamX
# -----------------------------------------------------------------------------
#
# Better OS/400 detection: see Bugzilla 31132

#echo color
WHITE_COLOR="\E[1;37m";
RED_COLOR="\E[1;31m";
BLUE_COLOR='\E[1;34m';
GREEN_COLOR="\E[1;32m";
YELLOW_COLOR="\E[1;33m";
RES="\E[0m";

echo_r () {
    # Color red: Error, Failed
    [[ $# -ne 1 ]] && return 1
    # shellcheck disable=SC2059
    printf "[${BLUE_COLOR}Flink${RES}] ${RED_COLOR}$1${RES}\n"
}

echo_g () {
    # Color green: Success
    [[ $# -ne 1 ]] && return 1
    # shellcheck disable=SC2059
    printf "[${BLUE_COLOR}Flink${RES}] ${GREEN_COLOR}$1${RES}\n"
}

echo_y () {
    # Color yellow: Warning
    [[ $# -ne 1 ]] && return 1
    # shellcheck disable=SC2059
    printf "[${BLUE_COLOR}Flink${RES}] ${YELLOW_COLOR}$1${RES}\n"
}

echo_w () {
    # Color yellow: White
    [[ $# -ne 1 ]] && return 1
    # shellcheck disable=SC2059
    printf "[${BLUE_COLOR}Flink${RES}] ${WHITE_COLOR}$1${RES}\n"
}

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false
darwin=false
os400=false
hpux=false
# shellcheck disable=SC2006
case "`uname`" in
CYGWIN*) cygwin=true;;
Darwin*) darwin=true;;
OS400*) os400=true;;
HP-UX*) hpux=true;;
esac

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
PRGDIR=`dirname "$PRG"`

# shellcheck disable=SC2124
# shellcheck disable=SC2034
RUN_ARGS="$@"
#global variables....
# shellcheck disable=SC2006
# shellcheck disable=SC2164
APP_HOME=`cd "$PRGDIR/.." >/dev/null; pwd`
APP_BASE="$APP_HOME"
APP_CONF="$APP_BASE"/conf
APP_LOG="$APP_BASE"/logs
APP_LIB="$APP_BASE"/lib
# shellcheck disable=SC2034
APP_BIN="$APP_BASE"/bin
APP_TEMP="$APP_BASE"/temp
[[ ! -d "$APP_LOG" ]] && mkdir "${APP_LOG}" >/dev/null
[[ ! -d "$APP_TEMP" ]] && mkdir "${APP_TEMP}" >/dev/null

# For Cygwin, ensure paths are in UNIX format before anything is touched
if ${cygwin}; then
  # shellcheck disable=SC2006
  [[ -n "$APP_HOME" ]] && APP_HOME=`cygpath --unix "$APP_HOME"`
  # shellcheck disable=SC2006
  [[ -n "$APP_BASE" ]] && APP_BASE=`cygpath --unix "$APP_BASE"`
fi

# Ensure that neither APP_HOME nor APP_BASE contains a colon
# as this is used as the separator in the classpath and Java provides no
# mechanism for escaping if the same character appears in the path.
case ${APP_HOME} in
  *:*) echo "Using APP_HOME:   $APP_HOME";
       echo "Unable to start as APP_HOME contains a colon (:) character";
       exit 1;
esac
case ${APP_BASE} in
  *:*) echo "Using APP_BASE:   $APP_BASE";
       echo "Unable to start as APP_BASE contains a colon (:) character";
       exit 1;
esac

# For OS400
if ${os400}; then
  # Set job priority to standard for interactive (interactive - 6) by using
  # the interactive priority - 6, the helper threads that respond to requests
  # will be running at the same priority as interactive jobs.
  COMMAND='chgjob job('${JOBNAME}') runpty(6)'
  system "${COMMAND}"
  # Enable multi threading
  export QIBM_MULTI_THREADED=Y
fi

doStart() {
    local proper=""
    if [[ $# -eq 0 ]]; then
      proper="application.yml"
      echo_w "not input properties-file,use default application.yml"
    else
      #Solve the path problem, arbitrary path, ignore prefix, only take the content after conf/
      proper=$(echo "$1"|awk -F 'conf/' '{print $2}')
      shift;
    fi
    # flink properties file
    local app_proper=""
    if [[ -f "$APP_CONF/$proper" ]] ; then
       app_proper="$APP_CONF/$proper"
    else
       echo_r "Usage: properties file:$proper not exists!!! ";
       exit 1;
    fi

    # flink main jar...
    # shellcheck disable=SC2155
    local flink_jar="${APP_LIB}/$(basename "${APP_BASE}").jar"

    local param_cli="com.streamxhub.common.conf.ParameterCli"
    # shellcheck disable=SC2006
    # shellcheck disable=SC2155
    local app_name="`java -cp "${flink_jar}" $param_cli --name "${app_proper}"`"
    if [ x"${app_name}" == x"" ] ; then
       echo_r "Usage:yarnname must be set,pluase check your conf:${app_proper}"
       exit 1
    fi

    # shellcheck disable=SC2006
    # shellcheck disable=SC2155
    local detached_mode="`java -cp "${flink_jar}" $param_cli --detached "${app_proper}"` $*"
    # shellcheck disable=SC2006
    # shellcheck disable=SC2155
    local resource_params="`java -cp "${flink_jar}" $param_cli --resource "${app_proper}"` $*"
    # shellcheck disable=SC2006
    # shellcheck disable=SC2155
    local dynamic_params="`java -cp "${flink_jar}" $param_cli --dynamic "${app_proper}"`"

    if [ x"${detached_mode// /}" == x"true" ] ; then
      echo_g "${app_name} Starting by:<Detached> mode"
      flink run  "$resource_params" "$dynamic_params" --jarfile "$flink_jar" --flink.conf "$app_proper"
      echo "${app_name}" > "${APP_TEMP}/.running"
    else
      echo_g "${app_name} Starting by:<Attach> mode"
      # shellcheck disable=SC2006
      # shellcheck disable=SC2155
      local app_log_date=`date "+%Y%m%d_%H%M%S"`
      local app_out="${APP_LOG}/${app_name}-${app_log_date}.log"
      flink run "$resource_params" "$dynamic_params" --jarfile "$flink_jar" --flink.conf "$app_proper" >> "$app_out" 2>&1 &
      echo "${app_name}" > "${APP_TEMP}/.running"
      echo_g "${app_name} starting,more detail please log:${app_out}"
    fi
}

doStop() {
  # shellcheck disable=SC2155
  # shellcheck disable=SC2034
  local running_app=$(cat "${APP_TEMP/.running}")
  if [ x"${running_app}" != x"" ]; then
    echo_w "can not found flink job!"
  fi
  flink list -r|grep "${running_app}"|awk '{print $4}'|xargs flink cancel
}

case "$1" in
    start)
       shift
       doStart "$@"
       exit $?
        ;;
    stop)
      doStop
      exit $?
      ;;
    *)
      echo_g "Unknown command: $1"
      echo_g "commands:"
      echo_g "  start             Start"
      echo_g "  stop              Stop"
      echo_g "                    are you running?"
      exit 1
    ;;
esac
