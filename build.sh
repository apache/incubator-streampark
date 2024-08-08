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
  RESET=$(printf '\033[0m')
else
  PRIMARY=""
  RED=""
  GREEN=""
  YELLOW=""
  BLUE=""
  RESET=""
fi

echo_r () {
  # Color red: Error, Failed
  [[ $# -ne 1 ]] && return 1
  # shellcheck disable=SC2059
  printf "[%sStreamPark%s] %s$1%s\n"  $BLUE $RESET $RED $RESET
}

echo_y () {
  # Color yellow: Warning
  [[ $# -ne 1 ]] && return 1
  # shellcheck disable=SC2059
  printf "[%sStreamPark%s] %s$1%s\n"  $BLUE $RESET $YELLOW $RESET
}

echo_g () {
  # Color green: Success
  [[ $# -ne 1 ]] && return 1
  # shellcheck disable=SC2059
  printf "[%sStreamPark%s] %s$1%s\n"  $BLUE $RESET $GREEN $RESET
}

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false;
darwin=false;
mingw=false
case "$(uname)" in
  CYGWIN*) cygwin=true ;;
  MINGW*) mingw=true;;
  Darwin*) darwin=true
    # Use /usr/libexec/java_home if available, otherwise fall back to /Library/Java/Home
    # See https://developer.apple.com/library/mac/qa/qa1170/_index.html
    if [[ -z "$JAVA_HOME" ]]; then
      if [[ -x "/usr/libexec/java_home" ]]; then
        JAVA_HOME="$(/usr/libexec/java_home)"; export JAVA_HOME
      else
        JAVA_HOME="/Library/Java/Home"; export JAVA_HOME
      fi
    fi
    ;;
esac

if [[ -z "$JAVA_HOME" ]]; then
  if [[ -r /etc/gentoo-release ]]; then
    JAVA_HOME=$(java-config --jre-home)
  fi
fi

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin ; then
  [[ -n "$JAVA_HOME" ]] &&
    JAVA_HOME=$(cygpath --unix "$JAVA_HOME")
  [[ -n "$CLASSPATH" ]] &&
    CLASSPATH=$(cygpath --path --unix "$CLASSPATH")
fi

# For Mingw, ensure paths are in UNIX format before anything is touched
if $mingw ; then
  [[ -n "$JAVA_HOME" ]] && [[ -d "$JAVA_HOME" ]] &&
    JAVA_HOME="$(cd "$JAVA_HOME" || (echo "cannot cd into $JAVA_HOME."; exit 1); pwd)"
fi

if [[ -z "$JAVA_HOME" ]]; then
  javaExecutable="$(which javac)"
  if [[ -n "$javaExecutable" ]] && ! [[ "$(expr "\"$javaExecutable\"" : '\([^ ]*\)')" = "no" ]]; then
    # readlink(1) is not available as standard on Solaris 10.
    readLink=$(which readlink)
    if [[ ! "$(expr "$readLink" : '\([^ ]*\)')" = "no" ]]; then
      if $darwin ; then
        javaHome="$(dirname "\"$javaExecutable\"")"
        javaExecutable="$(cd "\"$javaHome\"" && pwd -P)/javac"
      else
        javaExecutable="$(readlink -f "\"$javaExecutable\"")"
      fi
      javaHome="$(dirname "\"$javaExecutable\"")"
      javaHome=$(expr "$javaHome" : '\(.*\)/bin')
      JAVA_HOME="$javaHome"
      export JAVA_HOME
    fi
  fi
fi

if [[ -z "$JAVA_HOME" ]]; then
  echo "Warning: JAVA_HOME environment variable is not set."
fi

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
  printf '      %s   Version:  2.2.0-SNAPSHOT %s\n'                                                $BLUE   $RESET
  printf '      %s   WebSite:  https://streampark.apache.org%s\n'                                  $BLUE   $RESET
  printf '      %s   GitHub :  http://github.com/apache/streampark%s\n\n'                          $BLUE   $RESET
  printf '      %s   ──────── Apache StreamPark, Make stream processing easier ô~ô!%s\n\n'         $PRIMARY  $RESET
}

build() {
  if [[ -x "$PRG_DIR/mvnw" ]]; then
    echo_g "Apache StreamPark, building..."
    "$PRG_DIR/mvnw" -Pshaded,webapp,dist -DskipTests clean install
    if [[ $? -eq 0 ]]; then
      printf '\n'
      echo_g """StreamPark project build successful!
      dist: $(cd "$PRG_DIR" &>/dev/null && pwd)/dist\n"""
    fi
  else
    echo_r "permission denied: $PRG_DIR/mvnw, please check."
    exit 1
  fi
}

main() {
  print_logo
  build
}

main "$@"
