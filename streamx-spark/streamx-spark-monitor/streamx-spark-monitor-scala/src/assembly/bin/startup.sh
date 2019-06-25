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
# Control Script for the APOLLO Server
#
# Environment Variable Prerequisites
#
#   APOLLO_HOME   May point at your apollo "build" directory.
#
#   APOLLO_BASE   (Optional) Base directory for resolving dynamic portions
#                   of a apollo installation.  If not present, resolves to
#                   the same directory that APOLLO_HOME points to.
#
#   APOLLO_CONF    (Optional) config path
#
#   APOLLO_PID    (Optional) Path of the file which should contains the pid
#                   of the apollo startup java process, when start (fork) is
#                   used
# -----------------------------------------------------------------------------

#echo color
WHITE_COLOR="\E[1;37m";
RED_COLOR="\E[1;31m";
BLUE_COLOR='\E[1;34m';
GREEN_COLOR="\E[1;32m";
YELLOW_COLOR="\E[1;33m";
RES="\E[0m";

printf "${GREEN_COLOR}                                                                                   ${RES}\n"
printf "${GREEN_COLOR}                _____       __   __          ___                ____               ${RES}\n"
printf "${GREEN_COLOR}               / ___/____  / /__/ /_        /   |  ____  ____  / / ____            ${RES}\n"
printf "${GREEN_COLOR}               \__ \/ __ |/ //_/ __ \______/ /| | / __ \/ __ \/ / / __ \           ${RES}\n"
printf "${GREEN_COLOR}              ___/ / /_/ / ,< / /_/ /_____/ ___ |/ /_/ / /_/ / / / /_/ /           ${RES}\n"
printf "${GREEN_COLOR}             /____/\__, /_/|_/_.___/     /_/  |_/ .___/\____/_/_/\____/            ${RES}\n"
printf "${GREEN_COLOR}                     /_/                       /_/                                 ${RES}\n"
printf "${GREEN_COLOR}                                                                                   ${RES}\n"
printf "${GREEN_COLOR}                              ----- 省钱快报大数据团队荣誉出品 ^_^                     ${RES}\n\n"


echo_r () {
    # Color red: Error, Failed
    [[ $# -ne 1 ]] && return 1
    printf "[${BLUE_COLOR}apollo${RES}] ${RED_COLOR}$1${RES}\n"
}

echo_g () {
    # Color green: Success
    [[ $# -ne 1 ]] && return 1
    printf "[${BLUE_COLOR}apollo${RES}] ${GREEN_COLOR}$1${RES}\n"
}

echo_y () {
    # Color yellow: Warning
    [[ $# -ne 1 ]] && return 1
    printf "[${BLUE_COLOR}apollo${RES}] ${YELLOW_COLOR}$1${RES}\n"
}

echo_w () {
    # Color yellow: White
    [[ $# -ne 1 ]] && return 1
    printf "[${BLUE_COLOR}apollo${RES}] ${WHITE_COLOR}$1${RES}\n"
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

[[ -z "$APOLLO_HOME" ]] && APOLLO_HOME=`cd "$PRGDIR/.." >/dev/null; pwd`
[[ -z "$APOLLO_BASE" ]] && APOLLO_BASE="$APOLLO_HOME"
[[ -z "$APOLLO_CONF" ]] && APOLLO_CONF="$APOLLO_BASE"/conf
[[ -z "$APOLLO_OUT" ]] && APOLLO_OUT="$APOLLO_BASE"/logs/apollo.out
[[ -z "$APOLLO_TMPDIR" ]]  && APOLLO_TMPDIR="$APOLLO_BASE"/temp

# Ensure that any user defined CLASSPATH variables are not used on startup,
# but allow them to be specified in setenv.sh, in rare case when it is needed.
CLASSPATH=

if [[ -r "$APOLLO_BASE/bin/setenv.sh" ]]; then
  . "$APOLLO_BASE/bin/setenv.sh"
elif [[ -r "$APOLLO_HOME/bin/setenv.sh" ]]; then
  . "$APOLLO_HOME/bin/setenv.sh"
fi

# For Cygwin, ensure paths are in UNIX format before anything is touched
if ${cygwin}; then
  [[ -n "$JAVA_HOME" ]] && JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
  [[ -n "$JRE_HOME" ]] && JRE_HOME=`cygpath --unix "$JRE_HOME"`
  [[ -n "$APOLLO_HOME" ]] && APOLLO_HOME=`cygpath --unix "$APOLLO_HOME"`
  [[ -n "$APOLLO_BASE" ]] && APOLLO_BASE=`cygpath --unix "$APOLLO_BASE"`
  [[ -n "$CLASSPATH" ]] && CLASSPATH=`cygpath --path --unix "$CLASSPATH"`
fi

# Ensure that neither APOLLO_HOME nor APOLLO_BASE contains a colon
# as this is used as the separator in the classpath and Java provides no
# mechanism for escaping if the same character appears in the path.
case ${APOLLO_HOME} in
  *:*) echo "Using APOLLO_HOME:   $APOLLO_HOME";
       echo "Unable to start as APOLLO_HOME contains a colon (:) character";
       exit 1;
esac
case ${APOLLO_BASE} in
  *:*) echo "Using APOLLO_BASE:   $APOLLO_BASE";
       echo "Unable to start as APOLLO_BASE contains a colon (:) character";
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

# Get standard Java environment variables
if ${os400}; then
  # -r will Only work on the os400 if the files are:
  # 1. owned by the user
  # 2. owned by the PRIMARY group of the user
  # this will not work if the user belongs in secondary groups
  . "$APOLLO_HOME"/bin/setclasspath.sh
else
  if [[ -r "$APOLLO_HOME"/bin/setclasspath.sh ]]; then
    . "$APOLLO_HOME"/bin/setclasspath.sh
  else
    echo "Cannot find $APOLLO_HOME/bin/setclasspath.sh"
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

#check openjdk
if [[ "`${RUNJAVA} -version 2>&1 | head -1|grep "openjdk"|wc -l`"x == "1"x ]]; then
  echo_r "ERROR: please uninstall OpenJDK and install jdk first"
  exit 1;
fi



APOLLO_PIDDIR="/var/run";
if [[ ! -d "$APOLLO_PIDDIR" ]] ; then
    mkdir ${APOLLO_PIDDIR};
fi
APOLLO_PID="$APOLLO_BASE/apollo.pid";

# Add on extra jar files to CLASSPATH
if [[ ! -z "$CLASSPATH" ]] ; then
  CLASSPATH="$CLASSPATH":
fi
CLASSPATH="$CLASSPATH"

# Bugzilla 37848: When no TTY is available, don't output to console
have_tty=0
if [[ "`tty`" != "not a tty" ]]; then
    have_tty=1
fi

# Bugzilla 37848: When no TTY is available, don't output to console
have_tty=0
if [[ "`tty`" != "not a tty" ]]; then
    have_tty=1
fi

# For Cygwin, switch paths to Windows format before running java
if ${cygwin}; then
  JAVA_HOME=`cygpath --absolute --windows "$JAVA_HOME"`
  JRE_HOME=`cygpath --absolute --windows "$JRE_HOME"`
  APOLLO_HOME=`cygpath --absolute --windows "$APOLLO_HOME"`
  APOLLO_BASE=`cygpath --absolute --windows "$APOLLO_BASE"`
  CLASSPATH=`cygpath --path --windows "$CLASSPATH"`
fi

# ----- Execute The Requested Command -----------------------------------------

# Bugzilla 37848: only output this if we have a TTY
if [[ ${have_tty} -eq 1 ]]; then
  echo_w "Using APOLLO_BASE:   $APOLLO_BASE"
  echo_w "Using APOLLO_HOME:   $APOLLO_HOME"
  if [[ "$1" = "debug" ]] ; then
    echo_w "Using JAVA_HOME:      $JAVA_HOME"
  else
    echo_w "Using JRE_HOME:       $JRE_HOME"
  fi
  if [[ ! -z "$APOLLO_PID" ]]; then
    echo_w "Using APOLLO_PID:    $APOLLO_PID"
  fi
fi

PROPER=${APOLLO_CONF}/application.yml
[[ ! -f "${PROPER}" ]] && { echo "Usage: base application.yml not found ";exit; }
MAIN=$(grep 'app.main' ${PROPER} | grep -v '^#' | awk -F':' '{print $2}')

JARS=`ls -1 "$APOLLO_BASE"/lib`
for JAR in ${JARS}; do
    CLASSPATH=${CLASSPATH}:${APOLLO_BASE}/lib/${JAR}
done

JAVA_OPTS="""
-server
-Xms1024m
-Xmx1024m
-Xmn256m
-XX:NewSize=100m
-XX:+UseConcMarkSweepGC
-XX:CMSInitiatingOccupancyFraction=70
-XX:PermSize=128m
-XX:MaxPermSize=128m
-XX:ThreadStackSize=512
-Xloggc:${APOLLO_HOME}/logs/gc.log"""

eval "${RUNJAVA}" \
    ${JAVA_OPTS} \
    -classpath "\"${CLASSPATH}\"" \
    -Dapollo.home="${APOLLO_HOME}" \
    -Dspring.config.location="${PROPER}" \
    ${MAIN} >> ${APOLLO_OUT} 2>&1 &

exit 0;