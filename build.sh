#!/bin/bash
# ----------------------------------------------------------------------------
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
# ----------------------------------------------------------------------------

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

print_logo() {
  printf '\n'
  printf '      %s    _____ __                                             __       %s\n'          $PRIMARY $RESET
  printf '      %s   / ___// /_________  ____ _____ ___  ____  ____ ______/ /__     %s\n'          $PRIMARY $RESET
  printf '      %s   \__ \/ __/ ___/ _ \/ __ `/ __ `__ \/ __ \  __ `/ ___/ //_/     %s\n'          $PRIMARY $RESET
  printf '      %s  ___/ / /_/ /  /  __/ /_/ / / / / / / /_/ / /_/ / /  / ,<        %s\n'          $PRIMARY $RESET
  printf '      %s /____/\__/_/   \___/\__,_/_/ /_/ /_/ ____/\__,_/_/  /_/|_|       %s\n'          $PRIMARY $RESET
  printf '      %s                                   /_/                            %s\n\n'        $PRIMARY $RESET
  printf '      %s   Version:  2.0.0 %s\n'                                                         $BLUE   $RESET
  printf '      %s   WebSite:  https://streampark.apache.org%s\n'                                   $BLUE   $RESET
  printf '      %s   GitHub :  http://github.com/apache/streampark%s\n\n'                          $BLUE   $RESET
  printf '      %s   ──────── Apache StreamPark, Make stream processing easier ô~ô!%s\n\n'         $PRIMARY  $RESET
}

checkPerm() {
  if [ -x "$PRG_DIR/mvnw" ]; then
    return 0
  else
    return 1
  fi
}

selectScala() {
  echo_w 'StreamPark supports Scala 2.11 and 2.12. Which version do you need ?'
  select scala in "2.11" "2.12"
  do
    case $scala in
      "2.11")
        return 1
        ;;
      "2.12")
        return 2
        ;;
      *)
        echo_r "invalid selected, exit.."
        exit 1
        ;;
    esac
  done
}

selectMode() {
  echo_w 'StreamPark supports front-end and server-side mixed / detached packaging mode, Which mode do you need ?'
  select scala in "mixed mode" "detached mode"
  do
    case $scala in
      "mixed mode")
        return 1
        ;;
      "detached mode")
        return 2
        ;;
      *)
        echo_r "invalid selected, exit.."
        exit 1
        ;;
    esac
  done
}

mixedPackage() {
  scalaVer="2.11.12"
  binaryVer="2.11"
  if [ "$1" == 2 ]; then
    scalaVer="2.12.8"
    binaryVer="2.12"
  fi
  echo_g "build info: package mode @ mixed, scala version @ $binaryVer, now build starting..."
  "$PRG_DIR/mvnw" clean package -DskipTests -Dscala.version=$scalaVer -Dscala.binary.version=$binaryVer -Pwebapp

  if [ $? -eq 0 ]; then
     printf '\n'
     echo_g "StreamPark project build successful! build info: package mode @ mixed, scala version @ $binaryVer\n"
  fi
}

detachedPackage () {
  scalaVer="2.11.12"
  binaryVer="2.11"
  if [ "$1" == 2 ]; then
    scalaVer="2.12.8"
    binaryVer="2.12"
  fi

  echo_g "build info: package mode @ detached, scala version @ $binaryVer, now build starting..."

  "$PRG_DIR"/mvnw clean package -DskipTests -Dscala.version=$scalaVer -Dscala.binary.version=$binaryVer

  if [ $? -eq 0 ]; then
    printf '\n'
    echo_g """StreamPark project build successful! build info: package mode @ detached, scala version @ $binaryVer
    Next, you need to build front-end by yourself. build cmd:

     1) cd $PRG_DIR/streampark-console/streampark-console-webapp
     2) npm install # or yarn install
     3) npm build   # or yarn build

    please visit: https://streampark.apache.org/docs/user-guide/deployment for more detail. \n"""
  fi
}

main() {
  print_logo
  checkPerm
  if [ $? -eq 1 ]; then
    # shellcheck disable=SC2006
    echo_r "permission denied: $PRG_DIR/mvnw, please check."
    exit 1
  fi
  selectMode
  if [ $? -eq 1 ]; then
    selectScala
    if [ $? -eq 1 ]; then
      mixedPackage 1
    else
      mixedPackage 2
    fi
  else
    selectScala
    if [ $? -eq 1 ]; then
      detachedPackage 1
    else
      detachedPackage 2
    fi
  fi
}

main "$@"
