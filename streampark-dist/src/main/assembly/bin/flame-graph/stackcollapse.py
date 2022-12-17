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

import argparse
import json

parser = argparse.ArgumentParser(description = 'Collapse stack trace logs to FlameGraph input file.')
parser.add_argument("--input", "-i", type=str, required=True, help="Input stack trace file")

args = parser.parse_args()
inputFile = args.input

with open(inputFile) as f:
    stacktraceDict = {}

    for line in f.readlines():
        stacktraceLog = json.loads(line)

        assert 'stacktrace' in stacktraceLog, "Malformated json. 'stacktrace' key doesn't exist."
        stacktrace = stacktraceLog['stacktrace']

        if len(stacktrace) == 0:
            continue

        assert 'count' in stacktraceLog, "Malformated json. 'count' key doesn't exist."
        count = stacktraceLog['count']

        key = ';'.join(list(reversed(stacktrace)))
        if key in stacktraceDict:
            stacktraceDict[key] += count
        else:
            stacktraceDict[key] = count

    for stacktraceItem in stacktraceDict:
        print("%s %i" % (stacktraceItem, stacktraceDict[stacktraceItem]))
