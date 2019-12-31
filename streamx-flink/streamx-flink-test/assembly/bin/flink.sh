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
    printf "[${BLUE_COLOR}Flink${RES}] ${RED_COLOR}$1${RES}\n"
}

echo_g () {
    # Color green: Success
    [[ $# -ne 1 ]] && return 1
    printf "[${BLUE_COLOR}Flink${RES}] ${GREEN_COLOR}$1${RES}\n"
}

echo_y () {
    # Color yellow: Warning
    [[ $# -ne 1 ]] && return 1
    printf "[${BLUE_COLOR}Flink${RES}] ${YELLOW_COLOR}$1${RES}\n"
}

echo_w () {
    # Color yellow: White
    [[ $# -ne 1 ]] && return 1
    printf "[${BLUE_COLOR}Flink${RES}] ${WHITE_COLOR}$1${RES}\n"
}

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false
darwin=false
os400=false
hpux=false
case "`uname`" in
CYGWIN*) cygwin=true;;
Darwin*) darwin=true;;
OS400*) os400=true;;
HP-UX*) hpux=true;;
esac

# resolve links - $0 may be a softlink
PRG="$0"

while [[ -h "$PRG" ]]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

# Get standard environment variables
PRGDIR=`dirname "$PRG"`

RUN_ARGS="$@"
#global variables....
APP_HOME=`cd "$PRGDIR/.." >/dev/null; pwd`
APP_BASE="$APP_HOME"
APP_CONF="$APP_BASE"/conf
APP_LOG="$APP_BASE"/logs
APP_LIB="$APP_BASE"/lib
APP_BIN="$APP_BASE"/bin
APP_TEMP="$APP_BASE"/temp
[[ ! -d "$APP_LOG" ]] && mkdir ${APP_LOG} >/dev/null
[[ ! -d "$APP_TEMP" ]] && mkdir ${APP_TEMP} >/dev/null

# For Cygwin, ensure paths are in UNIX format before anything is touched
if ${cygwin}; then
  [[ -n "$APP_HOME" ]] && APP_HOME=`cygpath --unix "$APP_HOME"`
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
  system ${COMMAND}
  # Enable multi threading
  export QIBM_MULTI_THREADED=Y
fi

chooseConf() {
read -p "Please select configFile to startup:
$1
exit

" ensure
echo "${ensure}"
}

doStart() {
    # flink main jar...
    local shellReader="com.streamxhub.flink.core.conf.ShellConfigReader"
    local main_jar="${APP_LIB}/$(basename ${APP_BASE}).jar"
    local javaProps=(`java -cp "${main_jar}" $shellReader --conf ${main_jar}`)
    index=0
    confs=$(for prop in $javaProps; do
       echo "$index) ${prop}"
       # shellcheck disable=SC2006
       index=`expr ${index} + 1`
    done)

    local sureProp=$(chooseConf "${confs}")

     if [[ x"${sureProp}" == x"" ]] ; then
         echo_w "Usage error."
         exit 1
     elif [[ x"${sureProp}" == x"exit" ]] ; then
         echo_w "exit startup."
         exit 0
     elif [[ -n "`echo "${sureProp}" | sed 's/[0-9]//g'`" ]] ; then
        echo_r "Usage error."
        exit 1;
     else
         conf=${javaProps[${sureProp}]}
         local run_params="$(java -cp ${main_jar} $shellReader --read ${main_jar} ${conf})"
         echo "flink run -m yarn-cluster $run_params $main_jar"
     fi
}


case "$1" in
    start)
       shift
       doStart "$@"
       exit $?
        ;;
    stop)
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
