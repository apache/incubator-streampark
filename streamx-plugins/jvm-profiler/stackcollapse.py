
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
