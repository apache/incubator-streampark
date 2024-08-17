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

# shellcheck disable=SC2317

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
  RED=$(printf '\033[31m')
  GREEN=$(printf '\033[32m')
  BLUE=$(printf '\033[34m')
  RESET=$(printf '\033[0m')
else
  RED=""
  GREEN=""
  BLUE=""
  RESET=""
fi

echo_r () {
  # Color red: Error, Failed
  [[ $# -ne 1 ]] && return 1
  # shellcheck disable=SC2059
  printf "[%sStreamPark%s] %s$1%s\n"  "$BLUE" "$RESET" "$RED" "$RESET"
}

echo_g () {
  # Color green: Success
  [[ $# -ne 1 ]] && return 1
  # shellcheck disable=SC2059
  printf "[%sStreamPark%s] %s$1%s\n"  "$BLUE" "$RESET" "$GREEN" "$RESET"
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
    if [ -z "$JAVA_HOME" ]; then
      if [ -x "/usr/libexec/java_home" ]; then
        JAVA_HOME="$(/usr/libexec/java_home)"; export JAVA_HOME
      else
        JAVA_HOME="/Library/Java/Home"; export JAVA_HOME
      fi
    fi
    ;;
esac

if [ -z "$JAVA_HOME" ] ; then
  if [ -r /etc/gentoo-release ] ; then
    JAVA_HOME=$(java-config --jre-home)
  fi
fi

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin ; then
  [ -n "$JAVA_HOME" ] &&
    JAVA_HOME=$(cygpath --unix "$JAVA_HOME")
  [ -n "$CLASSPATH" ] &&
    CLASSPATH=$(cygpath --path --unix "$CLASSPATH")
fi

# For Mingw, ensure paths are in UNIX format before anything is touched
if $mingw ; then
  [ -n "$JAVA_HOME" ] && [ -d "$JAVA_HOME" ] &&
    JAVA_HOME="$(cd "$JAVA_HOME" || (echo_r "cannot cd into $JAVA_HOME."; exit 1); pwd)"
fi

if [ -z "$JAVA_HOME" ]; then
  javaExecutable="$(which javac)"
  if [ -n "$javaExecutable" ] && ! [ "$(expr "\"$javaExecutable\"" : '\([^ ]*\)')" = "no" ]; then
    # readlink(1) is not available as standard on Solaris 10.
    readLink=$(which readlink)
    if [ ! "$(expr "$readLink" : '\([^ ]*\)')" = "no" ]; then
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

if [ -z "$JAVACMD" ] ; then
  if [ -n "$JAVA_HOME"  ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
      # IBM's JDK on AIX uses strange locations for the executables
      JAVACMD="$JAVA_HOME/jre/sh/java"
    else
      JAVACMD="$JAVA_HOME/bin/java"
    fi
  else
    JAVACMD="$(\unset -f command 2>/dev/null; \command -v java)"
  fi
fi

if [ ! -x "$JAVACMD" ] ; then
  echo_r "Error: JAVA_HOME is not defined correctly." >&2
  echo_r "  We cannot execute $JAVACMD" >&2
  exit 1
fi

if [ -z "$JAVA_HOME" ] ; then
  echo_r "Warning: JAVA_HOME environment variable is not set."
fi

_RUNJAVA="$JAVA_HOME/bin/java"

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
WORK_DIR=$(cd "$PRG_DIR" >/dev/null || exit; pwd)

SP_VERSION="2.1.5"
SP_NAME="apache-streampark_2.12-${SP_VERSION}-incubating-bin"
SP_TAR="${SP_NAME}.tar.gz"
SP_URL="https://archive.apache.org/dist/incubator/streampark/${SP_VERSION}/${SP_TAR}"
SP_HOME="${WORK_DIR}"/"${SP_NAME}"
SP_PATH="${WORK_DIR}"/"${SP_TAR}"
SP_CONFIG="${SP_HOME}/conf/config.yaml"

download() {
  local url=$1
  local name=$2
  local path=$3
  if command -v wget > /dev/null; then
    wget "$url" -O "$path" || rm -f "$path"
    # shellcheck disable=SC2181
    if [[ $? -ne 0 ]]; then
      echo_r "download $name failed, please try again."
      exit 1
    fi
  elif command -v curl > /dev/null; then
    curl -o "$path" "$url" -f -L || rm -f "$path"
    # shellcheck disable=SC2181
    if [[ $? -ne 0 ]]; then
      echo_r "download $name failed, please try again."
      exit 1
    fi
  else
    echo "
      import java.io.InputStream;
      import java.net.URL;
      import java.nio.file.Files;
      import java.nio.file.Path;
      import java.nio.file.Paths;
      import java.nio.file.StandardCopyOption;

      public class Downloader {
        public static void main(String[] args) {
          try {
            URL url = new URL(args[0]);
            Path path = Paths.get(args[1]).toAbsolutePath().normalize();
            try (InputStream inStream = url.openStream()) {
              Files.copy(inStream, path, StandardCopyOption.REPLACE_EXISTING);
            }
          } catch (Exception e) {
            System.exit(1);
          }
        }
      }" > "${WORK_DIR}"/Downloader.java

    "$JAVA_HOME/bin/javac" "${WORK_DIR}"/Downloader.java && rm -f "${WORK_DIR}"/Downloader.java

    "$JAVA_HOME/bin/java" -cp "${WORK_DIR}" Downloader "$url" "$path" && rm -f "${WORK_DIR}"/Downloader.class

    if [[ $? -ne 0 ]]; then
      echo_r "download $name failed, please try again."
      exit 1
    fi
  fi
}

BASH_UTIL="org.apache.streampark.console.base.util.BashJavaUtils"

# 1). download streampark.
echo_g "download streampark..."

download "$SP_URL" "$SP_TAR" "$SP_PATH"
tar -xvf "${SP_TAR}" >/dev/null 2>&1 \
    && rm -r "${SP_TAR}" \
    && mkdir "${SP_HOME}"/flink \
    && mkdir "${SP_HOME}"/workspace

# 1.1) workspace
$_RUNJAVA -cp "${SP_HOME}/lib/*" $BASH_UTIL --replace "$SP_CONFIG" "local: ||local: ${SP_HOME}/workspace  #"

# 1.2) port.
SP_PORT=$($_RUNJAVA -cp "${SP_HOME}/lib/*" $BASH_UTIL --free_port "10000")
$_RUNJAVA -cp "${SP_HOME}/lib/*" $BASH_UTIL --replace "$SP_CONFIG" "port: 10000||port: ${SP_PORT}"

# 2). flink
# shellcheck disable=SC2009
FLINK_PROCESS="$(ps -ef | grep "flink-dist-" | grep 'org.apache.flink.runtime.entrypoint.StandaloneSessionClusterEntrypoint')"
if [[ -n "${FLINK_PROCESS}" ]]; then
  FLINK_PARAM=$($_RUNJAVA -cp "${SP_HOME}/lib/*" $BASH_UTIL --read_flink "$FLINK_PROCESS")
  # shellcheck disable=SC2206
  ARRAY=(${FLINK_PARAM//,/ })
  FLINK_HOME=${ARRAY[0]}
  FLINK_NAME=${ARRAY[1]}
  FLINK_PORT=${ARRAY[2]}
else
  FLINK_NAME="flink-1.19.0"
  FLINK_URL="https://archive.apache.org/dist/flink/${FLINK_NAME}/${FLINK_NAME}-bin-scala_2.12.tgz"
  FLINK_TAR="${FLINK_NAME}-bin-scala_2.12.tgz"
  FLINK_HOME="${WORK_DIR}"/${SP_NAME}/flink/${FLINK_NAME}
  FLINK_PATH="${WORK_DIR}"/"${FLINK_TAR}"

  # 1) download flink
  echo_g "download flink..."
  download "$FLINK_URL" "$FLINK_TAR" "$FLINK_PATH"
  tar -xvf "${FLINK_TAR}" >/dev/null 2>&1 \
    && rm -r "${FLINK_TAR}" \
    && mv "$FLINK_NAME" "${WORK_DIR}"/"${SP_NAME}"/flink

  # 2) start flink-cluster
  FLINK_PORT=$($_RUNJAVA -cp "${SP_HOME}/lib/*" $BASH_UTIL --free_port "8081")
  $_RUNJAVA -cp "${SP_HOME}/lib/*" $BASH_UTIL --replace "$SP_CONFIG" "# port: 8081||port: ${FLINK_PORT}"

  bash +x "${FLINK_HOME}"/bin/start-cluster.sh
fi

# 3) start streampark
bash +x "${SP_HOME}"/bin/startup.sh \
  --quickstart flink_home="$FLINK_HOME" \
  --quickstart flink_port="$FLINK_PORT" \
  --quickstart flink_name="quickstart-$FLINK_NAME"
