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
# Control Script for the StreamX Server
#
# Environment Variable Prerequisites
#
#   APP_HOME   May point at your StreamX "build" directory.
#
#   APP_BASE   (Optional) Base directory for resolving dynamic portions
#                   of a StreamX installation.  If not present, resolves to
#                   the same directory that APP_HOME points to.
#
#   APP_CONF    (Optional) config path
#
#   APP_PID    (Optional) Path of the file which should contains the pid
#                   of the StreamX startup java process, when start (fork) is
#                   used
# -----------------------------------------------------------------------------

#echo color
RED_COLOR="\E[1;31m";
GREEN_COLOR="\E[1;32m";
YELLOW_COLOR="\E[1;33m";
BLUE_COLOR='\E[1;34m';
WHITE_COLOR="\E[1;37m";
RES="\E[0m";

printf "\n\n"
printf "${RED_COLOR}                .+.                                         ${RES}\n"
printf "${RED_COLOR}          _____/ /_________  ____ _____ ___  _  __          ${RES}\n"
printf "${RED_COLOR}         / ___/ __/ ___/ _ \/ __ \`/ __ \`__ \| |/_/        ${RES}\n"
printf "${RED_COLOR}        (__  ) /_/ /  /  __/ /_/ / / / / / />   <           ${RES}\n"
printf "${RED_COLOR}       /____/\__/_/   \___/\__,_/_/ /_/ /_/_/|_|            ${RES}\n"
printf "${RED_COLOR}                                             |/             ${RES}\n"
printf "${RED_COLOR}                                             .              ${RES}\n\n"
printf "${BLUE_COLOR}       WebSite:  http://www.streamxhub.com                 ${RES}\n"
printf "${BLUE_COLOR}       GitHub :  https://github.com/streamxhub/streamx     ${RES}\n"
printf "${BLUE_COLOR}       Gitee  :  https://gitee.com/streamxhub/streamx      ${RES}\n\n"
printf "${GREEN_COLOR}                ────────  Make Flink|Spark easier ô‿ô!     ${RES}\n\n\n"


echo_r () {
    # Color red: Error, Failed
    [[ $# -ne 1 ]] && return 1
    # shellcheck disable=SC2059
    printf "[${BLUE_COLOR}StreamX${RES}] ${RED_COLOR}$1${RES}\n"
}

echo_g () {
    # Color green: Success
    [[ $# -ne 1 ]] && return 1
    # shellcheck disable=SC2059
    printf "[${BLUE_COLOR}StreamX${RES}] ${GREEN_COLOR}$1${RES}\n"
}

echo_y () {
    # Color yellow: Warning
    [[ $# -ne 1 ]] && return 1
    # shellcheck disable=SC2059
    printf "[${BLUE_COLOR}StreamX${RES}] ${YELLOW_COLOR}$1${RES}\n"
}

echo_w () {
    # Color yellow: White
    [[ $# -ne 1 ]] && return 1
    # shellcheck disable=SC2059
    printf "[${BLUE_COLOR}StreamX${RES}] ${WHITE_COLOR}$1${RES}\n"
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
APP_BIN="$APP_BASE"/bin
APP_LIB="$APP_BASE"/lib
APP_LOG="$APP_BASE"/logs
APP_PID="$APP_BASE"/.pid
APP_OUT="$APP_LOG"/streamx.out
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

#check java exists.
$RUNJAVA >/dev/null 2>&1

if [[ $? -ne 1 ]];then
  echo_r "ERROR: java is not install,please install java first!"
  exit 1;
fi

# Add on extra jar files to CLASSPATH
# shellcheck disable=SC2236
if [[ ! -z "$CLASSPATH" ]] ; then
  CLASSPATH="$CLASSPATH":
fi
CLASSPATH="$CLASSPATH"

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

# ----- Execute The Requested Command -----------------------------------------

# shellcheck disable=SC2120
running() {
  if [ -f "$APP_PID" ]; then
    if [ -s "$APP_PID" ]; then
        # shellcheck disable=SC2046
        # shellcheck disable=SC2006
        kill -0 `cat "$APP_PID"` >/dev/null 2>&1
        # shellcheck disable=SC2181
        if [ $? -eq 0 ]; then
          return 1
        else
          return 0
        fi
    else
      return 0
    fi
  else
    return 0
  fi
}

# shellcheck disable=SC2120
start() {
  running
  # shellcheck disable=SC2181
  if [ $? -eq "1" ]; then
    # shellcheck disable=SC2006
    echo_r "StreamX is already running pid: `cat "$APP_PID"`"
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

  shift

  if [[ $# -eq 0 ]]; then
    #default application.yml
    PROPER="${APP_CONF}/application.yml"
    if [[ ! -f "$PROPER" ]] ; then
      PROPER="${APP_CONF}/application.properties"
      if [[ ! -f "$PROPER" ]] ; then
        echo_r "Usage: properties file (application.properties|application.yml) not found! ";
      else
        echo_g "Usage: properties file:application.properties ";
      fi
    else
      echo_g "Usage: properties file:application.yml ";
    fi
  else
    #Solve the path problem, arbitrary path, ignore prefix, only take the content after conf/
    PROPER=$(echo "$1"|awk -F 'conf/' '{print $2}')
    PROPER=${APP_CONF}/$PROPER
  fi

  # shellcheck disable=SC2126
  ymlFile=$(echo "${PROPER}"|grep "\.yml$"|wc -l)
  if [[ $ymlFile -eq 1 ]]; then
    #source yaml.sh
    # shellcheck disable=SC1090
    source "${APP_BIN}"/yaml.sh
    yaml_get "${PROPER}"
    # shellcheck disable=SC2046
    # shellcheck disable=SC2116
    # shellcheck disable=SC2154
    PROFILE=$(echo "${spring_profiles_active}")
    if [[ ! "${PROFILE}" == "" ]];then
      PROFILE="${APP_CONF}/application-${PROFILE}.yml"
      PROPER="${PROFILE},${PROPER}"
    fi
  else
    PROFILE=$(grep 'spring.profiles.active' "${PROPER}" | grep -v '^#' | awk -F'=' '{print $2}')
    if [[ ! "${PROFILE}" == "" ]];then
      PROPER="${APP_CONF}/application-${PROFILE}.properties"
      PROPER="${PROFILE},${PROPER}"
    fi
  fi

  if [ "${HADOOP_HOME}"x == ""x ]; then
    echo_r "ERROR: HADOOP_HOME is undefined on your system env,please check it."
  else
    echo_w "Using HADOOP_HOME:   ${HADOOP_HOME}"
  fi

  #
  # classpath options:
  # 1): java env (lib and jre/lib)
  # 2): Streamx
  # 3): hadoop conf

  APP_CLASSPATH=".:${JAVA_HOME}/lib:${JAVA_HOME}/jre/lib"
  # shellcheck disable=SC2206
  # shellcheck disable=SC2010
  JARS=$(ls "$APP_LIB"/*.jar | grep -v "$APP_LIB/streamx-flink-shims_.*.jar$")
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

  PARAM_CLI="com.streamxhub.streamx.flink.core.scala.conf.ParameterCli"
  # shellcheck disable=SC2034
  # shellcheck disable=SC2006
  vmOption=`$RUNJAVA -cp "$APP_CLASSPATH" $PARAM_CLI --vmopt`

  JAVA_OPTS="""
  $vmOption
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

  eval $NOHUP "\"$RUNJAVA\"" $JAVA_OPTS \
    -classpath "\"$APP_CLASSPATH\"" \
    -Dapp.home="\"${APP_HOME}\"" \
    -Dspring.config.location="\"${PROPER}\"" \
    -Djava.io.tmpdir="\"$APP_TMPDIR\"" \
    -Dpid="\"${APP_PID}\"" \
    com.streamxhub.streamx.console.StreamXConsole \
    >> "$APP_OUT" 2>&1 "&"

  SLEEP_INTERVAL=5

  STARTED=0
  while [ $SLEEP_INTERVAL -ge 0 ]; do
    # shellcheck disable=SC2236
    if [ -f "$APP_PID" ]; then
      if [ -s "$APP_PID" ]; then
        echo_g "StreamX started. pid: `cat "$APP_PID"`"
        STARTED=1
        break
      fi
    fi
    if [ $SLEEP_INTERVAL -gt 0 ]; then
      sleep 1
    fi
    SLEEP_INTERVAL=`expr $SLEEP_INTERVAL - 1 `
  done

  if [ $STARTED -eq 0 ] ;then
    echo_g "StreamX started."
  fi
}

# shellcheck disable=SC2120
stop () {
  running
  # shellcheck disable=SC2181
  if [ $? -eq "0" ]; then
    echo_r "StreamX is not running."
    exit 1
  fi

  shift

  local SLEEP=5
  if [ ! -z "$1" ]; then
    echo $1 | grep "[^0-9]" >/dev/null 2>&1
    if [ $? -gt 0 ]; then
      SLEEP=$1
      shift
    fi
  fi

  local FORCE=0
  if [ "$1" = "-force" ]; then
    shift
    FORCE=1
  fi

  local STOPPED=0
  # shellcheck disable=SC2236
  if [ -f "$APP_PID" ]; then
    if [ -s "$APP_PID" ]; then
      # shellcheck disable=SC2046
      # shellcheck disable=SC2006
      kill -0 `cat "$APP_PID"` >/dev/null 2>&1
      # shellcheck disable=SC2181
      if [ $? -gt 0 ]; then
        echo "PID file found but either no matching process was found or the current user does not have permission to stop the process. Stop aborted."
        exit 1
      else
        kill -15 `cat "$APP_PID"` >/dev/null 2>&1
        if [ -f "$APP_PID" ]; then
          while [ $SLEEP -ge 0 ]; do
             if [ -f "$APP_PID" ]; then
               kill -0 `cat "$APP_PID"` >/dev/null 2>&1
               if [ $? -gt 0 ]; then
                 rm -f "$APP_PID" >/dev/null 2>&1
                 if [ $? != 0 ]; then
                   if [ -w "$APP_PID" ]; then
                     cat /dev/null > "$APP_PID"
                     # If StreamX has stopped don't try and force a stop with an empty PID file
                     FORCE=0
                   else
                     echo "The PID file could not be removed or cleared."
                   fi
                 fi
                 STOPPED=1
                 break
               fi
             else
               STOPPED=1
               break
             fi
             SLEEP=`expr $SLEEP - 1 `
          done

          #stop failed.normal kill failed? Try a force kill.
          if [ -f "$APP_PID" ]; then
            if [ -s "$APP_PID" ]; then
              kill -0 `cat "$APP_PID"` >/dev/null 2>&1
              if [ $? -eq 0 ]; then
                FORCE=1
              fi
            fi
          fi

          if [ $FORCE -eq 1 ]; then
            KILL_SLEEP_INTERVAL=5
            if [ -f "$APP_PID" ]; then
              PID=`cat "$APP_PID"`
              echo_y "Killing StreamX with the PID: $PID"
              kill -9 "$PID" >/dev/null 2>&1
              while [ $KILL_SLEEP_INTERVAL -ge 0 ]; do
                kill -0 `cat "$APP_PID"` >/dev/null 2>&1
                if [ $? -gt 0 ]; then
                  rm -f "$APP_PID" >/dev/null 2>&1
                  if [ $? != 0 ]; then
                    if [ -w "$APP_PID" ]; then
                      cat /dev/null > "$APP_PID"
                    else
                      echo_r "The PID file could not be removed."
                    fi
                  fi
                  echo_y "The StreamX process has been killed."
                  break
                fi
                if [ $KILL_SLEEP_INTERVAL -gt 0 ]; then
                  sleep 1
                fi
                KILL_SLEEP_INTERVAL=`expr $KILL_SLEEP_INTERVAL - 1 `
              done
              if [ $KILL_SLEEP_INTERVAL -lt 0 ]; then
                echo "StreamX has not been killed completely yet. The process might be waiting on some system call or might be UNINTERRUPTIBLE."
              fi
            fi
          fi
        else
          STOPPED=1
        fi
      fi
    else
      echo "PID file is empty and has been ignored."
      exit 1
    fi
  else
    echo "\$APP_PID was set but the specified file does not exist. Is StreamX running? Stop aborted."
    exit 1
  fi

  if [ $STOPPED -eq 1 ]; then
     echo_r "StreamX stopped."
  fi

}

status () {
  running
  # shellcheck disable=SC2181
  if [ $? -eq "1" ]; then
    # shellcheck disable=SC2006
    echo_g "StreamX is running pid is: `cat "$APP_PID"`"
  else
    echo_r "StreamX is not running"
  fi
}

restart () {
  # shellcheck disable=SC2119
  stop
  # shellcheck disable=SC2119
  start
}

case "$1" in
  "start")
      start
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
      echo_w "Usage: streamx.sh ( commands ... )"
      echo_w "commands:"
      echo_w "  start \$conf               Start StreamX with application config."
      echo_w "  stop n -force             Stop StreamX, wait up to n seconds and then use kill -KILL if still running"
      echo_w "  status                    StreamX status"
      echo_w "  restart \$conf             restart StreamX with application config."
      exit 0
      ;;
esac

exit 0;
