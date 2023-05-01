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
#
# -----------------------------------------------------------------------------
# Control Script for the StreamPark Server
#
# Environment Variable Prerequisites
#
#   APP_HOME   May point at your StreamPark "build" directory.
#
#   APP_BASE   (Optional) Base directory for resolving dynamic portions
#                   of a StreamPark installation.  If not present, resolves to
#                   the same directory that APP_HOME points to.
#
#   APP_CONF    (Optional) config path
#
#   APP_PID    (Optional) Path of the file which should contains the pid
#                   of the StreamPark startup java process, when start (fork) is
#                   used
# -----------------------------------------------------------------------------

# Bugzilla 37848: When no TTY is available, don't output to console
have_tty=0
# shellcheck disable=SC2006
if [[ "`tty`" != "not a tty" ]]; then
    have_tty=1
fi

# Bugzilla 37848: When no TTY is available, don't output to console
have_tty=0
# shellcheck disable=SC2006
if [[ "`tty`" != "not a tty" ]]; then
    have_tty=1
fi

 # Only use colors if connected to a terminal
if [[ ${have_tty} -eq 1 ]]; then
  PRIMARY=$(printf '\033[38;5;082m')
  RED=$(printf '\033[31m')
  GREEN=$(printf '\033[32m')
  YELLOW=$(printf '\033[33m')
  BLUE=$(printf '\033[34m')
  BOLD=$(printf '\033[1m')
  RESET=$(printf '\033[0m')
else
  PRIMARY=""
  RED=""
  GREEN=""
  YELLOW=""
  BLUE=""
  BOLD=""
  RESET=""
fi

echo_r () {
    # Color red: Error, Failed
    [[ $# -ne 1 ]] && return 1
    # shellcheck disable=SC2059
    printf "[%sStreamPark%s] %s$1%s\n"  $BLUE $RESET $RED $RESET
}

echo_g () {
    # Color green: Success
    [[ $# -ne 1 ]] && return 1
    # shellcheck disable=SC2059
    printf "[%sStreamPark%s] %s$1%s\n"  $BLUE $RESET $GREEN $RESET
}

echo_y () {
    # Color yellow: Warning
    [[ $# -ne 1 ]] && return 1
    # shellcheck disable=SC2059
    printf "[%sStreamPark%s] %s$1%s\n"  $BLUE $RESET $YELLOW $RESET
}

echo_w () {
    # Color yellow: White
    [[ $# -ne 1 ]] && return 1
    # shellcheck disable=SC2059
    printf "[%sStreamPark%s] %s$1%s\n"  $BLUE $RESET $WHITE $RESET
}

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false
os400=false
# shellcheck disable=SC2006
case "`uname`" in
CYGWIN*) cygwin=true;;
OS400*) os400=true;;
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
PRG_DIR=`dirname "$PRG"`

# shellcheck disable=SC2006
# shellcheck disable=SC2164
APP_HOME=`cd "$PRG_DIR/.." >/dev/null; pwd`
APP_BASE="$APP_HOME"
APP_CONF="$APP_BASE"/conf
APP_LIB="$APP_BASE"/lib
APP_LOG="$APP_BASE"/logs
APP_PID="$APP_BASE"/.pid
APP_OUT="$APP_LOG"/streampark.out
# shellcheck disable=SC2034
APP_TMPDIR="$APP_BASE"/temp

# Ensure that any user defined CLASSPATH variables are not used on startup,
# but allow them to be specified in setenv.sh, in rare case when it is needed.
CLASSPATH=

if [[ -r "$APP_BASE/bin/setenv.sh" ]]; then
  # shellcheck disable=SC1090
  . "$APP_BASE/bin/setenv.sh"
elif [[ -r "$APP_HOME/bin/setenv.sh" ]]; then
  # shellcheck disable=SC1090
  . "$APP_HOME/bin/setenv.sh"
fi

# For Cygwin, ensure paths are in UNIX format before anything is touched
if ${cygwin}; then
  # shellcheck disable=SC2006
  [[ -n "$JAVA_HOME" ]] && JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
  # shellcheck disable=SC2006
  [[ -n "$JRE_HOME" ]] && JRE_HOME=`cygpath --unix "$JRE_HOME"`
  # shellcheck disable=SC2006
  [[ -n "$APP_HOME" ]] && APP_HOME=`cygpath --unix "$APP_HOME"`
  # shellcheck disable=SC2006
  [[ -n "$APP_BASE" ]] && APP_BASE=`cygpath --unix "$APP_BASE"`
  # shellcheck disable=SC2006
  [[ -n "$CLASSPATH" ]] && CLASSPATH=`cygpath --path --unix "$CLASSPATH"`
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

# Get standard Java environment variables
if ${os400}; then
  # -r will Only work on the os400 if the files are:
  # 1. owned by the user
  # 2. owned by the PRIMARY group of the user
  # this will not work if the user belongs in secondary groups
  # shellcheck disable=SC1090
  . "$APP_HOME"/bin/setclasspath.sh
else
  if [[ -r "$APP_HOME"/bin/setclasspath.sh ]]; then
    # shellcheck disable=SC1090
    . "$APP_HOME"/bin/setclasspath.sh
  else
    echo "Cannot find $APP_HOME/bin/setclasspath.sh"
    echo "This file is needed to run this program"
    exit 1
  fi
fi

# Add on extra jar files to CLASSPATH
# shellcheck disable=SC2236
if [ ! -z "$CLASSPATH" ]; then
  CLASSPATH="$CLASSPATH":
fi
CLASSPATH="$CLASSPATH"

# For Cygwin, switch paths to Windows format before running java
if ${cygwin}; then
  # shellcheck disable=SC2006
  JAVA_HOME=`cygpath --absolute --windows "$JAVA_HOME"`
  # shellcheck disable=SC2006
  JRE_HOME=`cygpath --absolute --windows "$JRE_HOME"`
  # shellcheck disable=SC2006
  APP_HOME=`cygpath --absolute --windows "$APP_HOME"`
  # shellcheck disable=SC2006
  APP_BASE=`cygpath --absolute --windows "$APP_BASE"`
  # shellcheck disable=SC2006
  CLASSPATH=`cygpath --path --windows "$CLASSPATH"`
fi

if [ -z "$USE_NOHUP" ]; then
  if $hpux; then
    USE_NOHUP="true"
  else
    USE_NOHUP="false"
  fi
fi
unset NOHUP
if [ "$USE_NOHUP" = "true" ]; then
  NOHUP="nohup"
fi


PARAM_CLI="org.apache.streampark.flink.core.conf.ParameterCli"

APP_MAIN="org.apache.streampark.console.StreamParkConsoleBootstrap"

DEFAULT_OPTS="""
  -ea
  -server
  -Xms1024m
  -Xmx1024m
  -Xmn256m
  -XX:NewSize=100m
  -XX:+UseConcMarkSweepGC
  -XX:CMSInitiatingOccupancyFraction=70
  -XX:ThreadStackSize=512
  -Xloggc:${APP_HOME}/logs/gc.log
  """

DEBUG_OPTS=""

# ----- Execute The Requested Command -----------------------------------------

print_logo() {
  printf '\n'
  printf '      %s    _____ __                                             __       %s\n'          $PRIMARY $RESET
  printf '      %s   / ___// /_________  ____ _____ ___  ____  ____ ______/ /__     %s\n'          $PRIMARY $RESET
  printf '      %s   \__ \/ __/ ___/ _ \/ __ `/ __ `__ \/ __ \  __ `/ ___/ //_/     %s\n'          $PRIMARY $RESET
  printf '      %s  ___/ / /_/ /  /  __/ /_/ / / / / / / /_/ / /_/ / /  / ,<        %s\n'          $PRIMARY $RESET
  printf '      %s /____/\__/_/   \___/\__,_/_/ /_/ /_/ ____/\__,_/_/  /_/|_|       %s\n'          $PRIMARY $RESET
  printf '      %s                                   /_/                            %s\n\n'        $PRIMARY $RESET
  printf '      %s   Version:  2.1.0 %s\n'                                                $BLUE   $RESET
  printf '      %s   WebSite:  https://streampark.apache.org%s\n'                                   $BLUE   $RESET
  printf '      %s   GitHub :  http://github.com/apache/streampark%s\n\n'                          $BLUE   $RESET
  printf '      %s   ──────── Apache StreamPark, Make stream processing easier ô~ô!%s\n\n'         $PRIMARY  $RESET
}

parse_yaml() {
   local prefix=$2
   local s='[[:space:]]*' w='[a-zA-Z0-9_]*' fs=$(echo @|tr @ '\034')
   sed -ne "s|^\($s\):|\1|" \
        -e "s|^\($s\)\($w\)$s:$s[\"']\(.*\)[\"']$s\$|\1$fs\2$fs\3|p" \
        -e "s|^\($s\)\($w\)$s:$s\(.*\)$s\$|\1$fs\2$fs\3|p"  $1 |
   awk -F$fs '{
      indent = length($1)/2;
      vname[indent] = $2;
      for (i in vname) {if (i > indent) {delete vname[i]}}
      if (length($3) > 0) {
         vn=""; for (i=0; i<indent; i++) {vn=(vn)(vname[i])("_")}
         printf("%s%s%s=\"%s\"\n", "'$prefix'",vn, $2, $3);
      }
   }'
}

# shellcheck disable=SC2120
get_pid() {
  if [ -f "$APP_PID" ]; then
    if [ -s "$APP_PID" ]; then
      # shellcheck disable=SC2155
      # shellcheck disable=SC2006
      local PID=`cat "$APP_PID"`
      kill -0 $PID >/dev/null 2>&1
      # shellcheck disable=SC2181
      if [ $? -eq 0 ]; then
        echo $PID
        exit 0
      fi
    else
      rm -f "$APP_PID" >/dev/null 2>&1
    fi
  fi

  # shellcheck disable=SC2006
  local PROPER="${APP_CONF}/application.yml"
  if [[ ! -f "$PROPER" ]] ; then
    echo_r "ERROR: config file application.yml invalid or not found! ";
    exit 1;
  fi

  # shellcheck disable=SC2046
  eval $(parse_yaml "${PROPER}" "conf_")
  # shellcheck disable=SC2154
  # shellcheck disable=SC2155
  # shellcheck disable=SC2116
  local serverPort=$(echo "$conf_server_port")
  # shellcheck disable=SC2006
  # shellcheck disable=SC2155
  local used=`lsof -i:"$serverPort" | wc -l`
  if [ "$used" -gt 0 ]; then
    # shellcheck disable=SC2006
    local PID=`jps -l | grep "$APP_MAIN" | awk '{print $1}'`
    if [ ! -z $PID ]; then
      echo $PID
    else
      echo 0
    fi
  else
    echo 0
  fi
}

# shellcheck disable=SC2120
start() {
  # shellcheck disable=SC2006
  local PID=$(get_pid)

  if [ $PID -gt 0 ]; then
    # shellcheck disable=SC2006
    echo_r "StreamPark is already running pid: $PID , start aborted!"
    exit 1
  fi

  # Bugzilla 37848: only output this if we have a TTY
  if [[ ${have_tty} -eq 1 ]]; then
    echo_w "Using APP_BASE:   $APP_BASE"
    echo_w "Using APP_HOME:   $APP_HOME"
    if [[ "$1" = "debug" ]] ; then
      echo_w "Using JAVA_HOME:   $JAVA_HOME"
    else
      echo_w "Using JRE_HOME:   $JRE_HOME"
    fi
    echo_w "Using APP_PID:   $APP_PID"
  fi

  local PROPER="${APP_CONF}/application.yml"
  if [[ ! -f "$PROPER" ]] ; then
    echo_r "ERROR: config file application.yml invalid or not found! ";
    exit 1;
  else
    echo_g "Usage: config file: $PROPER ";
  fi

  # shellcheck disable=SC2046
  eval $(parse_yaml "${PROPER}" "conf_")
  # shellcheck disable=SC2001
  # shellcheck disable=SC2154
  # shellcheck disable=SC2155
  local workspace=$(echo "$conf_streampark_workspace_local" | sed 's/#.*$//g')
  if [[ ! -d $workspace ]]; then
    echo_r "ERROR: streampark.workspace.local: \"$workspace\" is invalid path, Please reconfigure in application.yml"
    echo_r "NOTE: \"streampark.workspace.local\" Do not set under APP_HOME($APP_HOME). Set it to a secure directory outside of APP_HOME.  "
    exit 1;
  fi
  if [[ ! -w $workspace ]] || [[ ! -r $workspace ]]; then
      echo_r "ERROR: streampark.workspace.local: \"$workspace\" Permission denied! "
      exit 1;
  fi

  if [ "${HADOOP_HOME}"x == ""x ]; then
    echo_y "WARN: HADOOP_HOME is undefined on your system env,please check it."
  else
    echo_w "Using HADOOP_HOME:   ${HADOOP_HOME}"
  fi

  #
  # classpath options:
  # 1): java env (lib and jre/lib)
  # 2): StreamPark
  # 3): hadoop conf
  # shellcheck disable=SC2091
  local APP_CLASSPATH=".:${JAVA_HOME}/lib:${JAVA_HOME}/jre/lib"
  # shellcheck disable=SC2206
  # shellcheck disable=SC2010
  local JARS=$(ls "$APP_LIB"/*.jar | grep -v "$APP_LIB/streampark-flink-shims_.*.jar$")
  # shellcheck disable=SC2128
  for jar in $JARS;do
    APP_CLASSPATH=$APP_CLASSPATH:$jar
  done

  if [[ -n "${HADOOP_CONF_DIR}" ]] && [[ -d "${HADOOP_CONF_DIR}" ]]; then
    echo_w "Using HADOOP_CONF_DIR:   ${HADOOP_CONF_DIR}"
    APP_CLASSPATH+=":${HADOOP_CONF_DIR}"
  else
    APP_CLASSPATH+=":${HADOOP_HOME}/etc/hadoop"
  fi

  # shellcheck disable=SC2034
  # shellcheck disable=SC2006
  local vmOption=`$_RUNJAVA -cp "$APP_CLASSPATH" $PARAM_CLI --vmopt`

  local JAVA_OPTS="""
  $vmOption
  $DEFAULT_OPTS
  $DEBUG_OPTS
  """

  eval $NOHUP $_RUNJAVA $JAVA_OPTS \
    -classpath "$APP_CLASSPATH" \
    -Dapp.home="${APP_HOME}" \
    -Dlogging.config="${APP_CONF}/logback-spring.xml" \
    -Dspring.config.location="${PROPER}" \
    -Djava.io.tmpdir="$APP_TMPDIR" \
    $APP_MAIN >> "$APP_OUT" 2>&1 "&"

    local PID=$!
    local IS_NUMBER="^[0-9]+$"

    # Add to pid file if successful start
    if [[ ${PID} =~ ${IS_NUMBER} ]] && kill -0 $PID > /dev/null 2>&1 ; then
        echo $PID > "$APP_PID"
        # shellcheck disable=SC2006
        echo_g "StreamPark start successful. pid: $PID"
    else
        echo_r "StreamPark start failed."
        exit 1
    fi
}

# shellcheck disable=SC2120
start_docker() {
  # Bugzilla 37848: only output this if we have a TTY
  if [[ ${have_tty} -eq 1 ]]; then
    echo_w "Using APP_BASE:   $APP_BASE"
    echo_w "Using APP_HOME:   $APP_HOME"
    if [[ "$1" = "debug" ]] ; then
      echo_w "Using JAVA_HOME:   $JAVA_HOME"
    else
      echo_w "Using JRE_HOME:   $JRE_HOME"
    fi
    echo_w "Using APP_PID:   $APP_PID"
  fi

  local PROPER="${APP_CONF}/application.yml"
  if [[ ! -f "$PROPER" ]] ; then
    echo_r "ERROR: config file application.yml invalid or not found! ";
    exit 1;
  else
    echo_g "Usage: config file: $PROPER ";
  fi

  if [ "${HADOOP_HOME}"x == ""x ]; then
    echo_y "WARN: HADOOP_HOME is undefined on your system env,please check it."
  else
    echo_w "Using HADOOP_HOME:   ${HADOOP_HOME}"
  fi

  # classpath options:
  # 1): java env (lib and jre/lib)
  # 2): StreamPark
  # 3): hadoop conf
  # shellcheck disable=SC2091
  local APP_CLASSPATH=".:${JAVA_HOME}/lib:${JAVA_HOME}/jre/lib"
  # shellcheck disable=SC2206
  # shellcheck disable=SC2010
  local JARS=$(ls "$APP_LIB"/*.jar | grep -v "$APP_LIB/streampark-flink-shims_.*.jar$")
  # shellcheck disable=SC2128
  for jar in $JARS;do
    APP_CLASSPATH=$APP_CLASSPATH:$jar
  done

  if [[ -n "${HADOOP_CONF_DIR}" ]] && [[ -d "${HADOOP_CONF_DIR}" ]]; then
    echo_w "Using HADOOP_CONF_DIR:   ${HADOOP_CONF_DIR}"
    APP_CLASSPATH+=":${HADOOP_CONF_DIR}"
  else
    APP_CLASSPATH+=":${HADOOP_HOME}/etc/hadoop"
  fi

  # shellcheck disable=SC2034
  # shellcheck disable=SC2006
  local vmOption=`$_RUNJAVA -cp "$APP_CLASSPATH" $PARAM_CLI --vmopt`

  local JAVA_OPTS="""
    $vmOption
    $DEFAULT_OPTS
    $DEBUG_OPTS
    """

  $_RUNJAVA $JAVA_OPTS \
    -classpath "$APP_CLASSPATH" \
    -Dapp.home="${APP_HOME}" \
    -Dlogging.config="${APP_CONF}/logback-spring.xml" \
    -Dspring.config.location="${PROPER}" \
    -Djava.io.tmpdir="$APP_TMPDIR" \
    $APP_MAIN

}

debug() {
  if [ ! -n "$DEBUG_PORT" ]; then
    echo_r "If start with debug mode,Please fill in the debug port like: bash streampark.sh debug 10002 "
  else
    DEBUG_OPTS="""
    -Xdebug  -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=$DEBUG_PORT
    """
    start
  fi
}

# shellcheck disable=SC2120
stop() {
  # shellcheck disable=SC2155
  # shellcheck disable=SC2006
  local PID=$(get_pid)

  if [[ $PID -eq 0 ]]; then
    echo_r "StreamPark is not running. stop aborted."
    exit 1
  fi

  shift

  local SLEEP=3

  # shellcheck disable=SC2006
  echo_g "StreamPark stopping with the PID: $PID"

  kill -9 $PID

  while [ $SLEEP -ge 0 ]; do
    # shellcheck disable=SC2046
    # shellcheck disable=SC2006
    kill -0 $PID >/dev/null 2>&1
    # shellcheck disable=SC2181
    if [ $? -gt 0 ]; then
      rm -f "$APP_PID" >/dev/null 2>&1
      if [ $? != 0 ]; then
        if [ -w "$APP_PID" ]; then
          cat /dev/null > "$APP_PID"
        else
          echo_r "The PID file could not be removed."
        fi
      fi
      echo_g "StreamPark stopped."
      break
    fi

    if [ $SLEEP -gt 0 ]; then
       sleep 1
    fi
    # shellcheck disable=SC2006
    # shellcheck disable=SC2003
    SLEEP=`expr $SLEEP - 1 `
  done

  if [ "$SLEEP" -lt 0 ]; then
     echo_r "StreamPark has not been killed completely yet. The process might be waiting on some system call or might be UNINTERRUPTIBLE."
  fi
}

status() {
  # shellcheck disable=SC2155
  # shellcheck disable=SC2006
  local PID=$(get_pid)
  if [ $PID -eq 0 ]; then
    echo_r "StreamPark is not running"
  else
    echo_g "StreamPark is running pid is: $PID"
  fi
}

restart() {
  # shellcheck disable=SC2119
  stop
  # shellcheck disable=SC2119
  start
}

main() {
  print_logo
  case "$1" in
    "debug")
        DEBUG_PORT=$2
        debug
        ;;
    "start")
        start
        ;;
    "start_docker")
        start_docker
        ;;
    "stop")
        stop
        ;;
    "status")
        status
        ;;
    "restart")
        restart
        ;;
    *)
        echo_r "Unknown command: $1"
        echo_w "Usage: streampark.sh ( commands ... )"
        echo_w "commands:"
        echo_w "  start \$conf               Start StreamPark with application config."
        echo_w "  stop                      Stop StreamPark, wait up to 3 seconds and then use kill -KILL if still running"
        echo_w "  status                    StreamPark status"
        echo_w "  debug                     StreamPark start with debug mode,start debug mode, like: bash streampark.sh debug 10002"
        echo_w "  restart \$conf             restart StreamPark with application config."
        exit 0
        ;;
  esac
}

main "$@"
