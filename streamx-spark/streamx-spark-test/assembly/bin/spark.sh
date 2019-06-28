#!/bin/bash
#
# Copyright (c) 2015 The StreamX Project
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
    printf "[${BLUE_COLOR}spark${RES}] ${RED_COLOR}$1${RES}\n"
}

echo_g () {
    # Color green: Success
    [[ $# -ne 1 ]] && return 1
    printf "[${BLUE_COLOR}spark${RES}] ${GREEN_COLOR}$1${RES}\n"
}

echo_y () {
    # Color yellow: Warning
    [[ $# -ne 1 ]] && return 1
    printf "[${BLUE_COLOR}spark${RES}] ${YELLOW_COLOR}$1${RES}\n"
}

echo_w () {
    # Color yellow: White
    [[ $# -ne 1 ]] && return 1
    printf "[${BLUE_COLOR}spark${RES}] ${WHITE_COLOR}$1${RES}\n"
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


chooseApp() {
read -p "Please select application index to shutdown:
$1
exit
" app_target
echo "${app_target}"
}

doStart() {
    ###########################################..spark env start...#####################################################################
    local proper=""
    if [[ $# -eq 0 ]]; then
      proper="application.properties"
      echo_w "not input properties-file,use default application.properties"
    else
      #Solve the path problem, arbitrary path, ignore prefix, only take the content after conf/
      proper=$(echo "$1"|awk -F 'conf/' '{print $2}')
    fi
    # spark properties file
    local app_proper=""
    if [[ -f "$APP_CONF/$proper" ]] ; then
       app_proper="$APP_CONF/$proper"
    else
       echo_r "Usage: properties file:$proper not exists!!! ";
       exit 1;
    fi
    #spark app name
    local app_name=$(grep 'spark.app.name' ${app_proper} | grep -v '^#' | awk -F'=' '{print $2}')
    # spark main jar...
    local main_jar="${APP_LIB}/$(basename ${APP_BASE}).jar"
    #spark main class
    local main=$(grep 'spark.main.class' ${app_proper} | grep -v '^#' | awk -F'=' '{print $2}')
    #spark main parameter..
    local main_params=$(grep 'spark.main.params' ${app_proper} | grep -v '^#' | awk -F'params=' '{print $2}')
    #spark application id file
    local app_pid="$APP_TEMP/${app_name}.pid"
    #spark application lock file
    local app_lock="$APP_TEMP/${app_name}.lock"

    shift
    local app_params=""
    if [[ "x$*" != "x" ]]; then
        app_params=$*
    fi

    ### assembly all jars.......
    for jar in $(ls -1 "${APP_LIB}"); do
        if [[ "x$jar" != "x$(basename ${main_jar})" ]]; then
            jars=${APP_LIB}/${jar},${jars}
        fi
    done

    [[ "x$jars" == "x" ]] && { echo_r "Usage: ${APP_LIB} assembly jar error!!! "; exit 1; }

    ##check lock.....
    if [[ -f "${app_lock}" ]] ; then
        echo_r "this app already running.please check it,you cat delete $app_lock by yourself...";
        exit 1
    else
        #create lock file
        touch ${app_lock}
    fi
    ###########################################..spark env end...#####################################################################

    local exit_code=0

    sudo -u hdfs yarn application --list | grep -i "spark" | grep "${app_name}" >/dev/null

    if [[ $? -eq 0 ]]; then
        echo_r "${app_name} already exists!!!"
        exit_code=1
    else
        echo_g "${app_name} Starting..."
        local app_log_date=`date "+%Y%m%d_%H%M%S"`
        local app_out="${APP_LOG}/${app_name}-${app_log_date}.log"

        sudo -u hdfs spark2-submit \
            --files ${app_proper} \
            --conf "spark.startup=${APP_BIN}/$0 $@" \
            --conf "spark.conf=${app_proper}" \
            --name ${app_name} \
            --queue spark \
            --jars ${jars} ${app_params}  \
            --class ${main}  ${main_jar} ${main_params} \
            >> ${app_out} 2>&1

        exit_code=$?

        if [[ ${exit_code} -eq 0 ]] ; then
             #get application_id
             local pid="application_`grep "tracking URL:" ${app_out}|awk -F'/application_' '{print $2}'|awk -F'/' '{print $1}'`"
             #write application_id to ${app_pid}
             echo ${pid} > ${app_pid}
             echo_g "${app_name} start successful,application_id:${pid}"
        else
            echo_r "${app_name} start error,please log:${app_out}"
        fi
    fi
     #start done,and delete lock file.....
    [[ -f "${app_lock}" ]] && rm -rf ${app_lock}
    exit ${exit_code}
}

doShutdown() {

    local exit_code=0

    local pid_len="`ls ${APP_TEMP}/ | grep ".pid"|wc -l`"

    if [[ x"${pid_len}" == x"0" ]] ; then
        echo_r "cat not found application_id!!!"
        exit_code=1
    else
        #only one pid...
        if [[ x"${pid_len}" == x"1" ]] ; then
           pid_file="`ls ${APP_TEMP}/ | grep ".pid"`"
           pid="`cat ${APP_TEMP}/${pid_file}`"
        else
           ###more pid file.....
           index=0
           for pid_file in `ls ${APP_TEMP}/ | grep ".pid"`; do
               pid_files[index]=${pid_file}
               index=`expr ${index} + 1`
           done
           index=0
           apps=$(for pid_file in `ls ${APP_TEMP}/ | grep ".pid"`; do
               pid_APP=$(echo "$pid_file"|awk -F'.pid' '{print $1}')
               echo "$index) ${pid_APP}"
               index=`expr ${index} + 1`
           done)

           local app_target=$(chooseApp "${apps}")

           if [[ x"${app_target}" == x"" ]] ; then
               echo_w "Usage error."
               exit 1
           elif [[ x"${app_target}" == x"exit" ]] ; then
               echo_w "exit shutdown."
               exit 0
           elif [[ -n "`echo ${app_target} | sed 's/[0-9]//g'`" ]] ; then
              echo_r "Usage error."
              exit 1;
           else
               pid_file=${pid_files[${app_target}]}
               pid=`cat ${APP_TEMP}/${pid_file}`
           fi
        fi

        sudo -u hdfs yarn application -kill ${pid}  >/dev/null

        if [[ $? -eq 0 ]] ; then
           echo_g "stop successful,application_id:${pid}"
           [[ -f "${APP_TEMP}/${pid_file}" ]] && rm -rf ${APP_TEMP}/${pid_file}
        else
           echo_r "stop error,application_id:${pid}"
        fi

        exit_code=$?

    fi

    exit ${exit_code}
}

case "$1" in
    start)
       shift
       doStart "$@"
       exit $?
        ;;
    stop)
      doShutdown
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
