#!/bin/bash

#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

#
# record-test.sh - Overwrite flame graph test result files.
#
# See test.sh, which checks these resulting files.
#
# Currently only tests stackcollapse-perf.pl.

set -v -x

# ToDo: add some form of --inline, and --inline --context tests. These are
# tricky since they use addr2line, whose output will vary based on the test
# system's binaries and symbol tables.
for opt in pid tid kernel jit all addrs; do
  for testfile in test/*.txt ; do
    echo testing $testfile : $opt
    outfile=${testfile#*/}
    outfile=test/results/${outfile%.txt}"-collapsed-${opt}.txt"
    ./stackcollapse-perf.pl --"${opt}" "${testfile}" 2> /dev/null > $outfile
  done
done
