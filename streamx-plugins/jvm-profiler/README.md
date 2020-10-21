# Uber JVM Profiler

[![Build Status](https://api.travis-ci.org/uber-common/jvm-profiler.svg)](https://travis-ci.org/uber-common/jvm-profiler/)

Uber JVM Profiler provides a Java Agent to collect various metrics and stacktraces for Hadoop/Spark JVM processes 
in a distributed way, for example, CPU/Memory/IO metrics. 

Uber JVM Profiler also provides advanced profiling capabilities to trace arbitrary Java methods and arguments on 
the user code without user code change requirement. This feature could be used to trace HDFS name node call latency 
for each Spark application and identify bottleneck of name node. It could also trace the HDFS file paths each Spark 
application reads or writes and identify hot files for further optimization.

This profiler is initially created to profile Spark applications which usually have dozens of or hundreds of 
processes/machines for a single application, so people could easily correlate metrics of these different 
processes/machines. It is also a generic Java Agent and could be used for any JVM process as well.

## How to Build

1. Make sure JDK 8+ and maven is installed on your machine.
2. Run: `mvn clean package`

This command creates **jvm-profiler.jar** file with the default reporters like ConsoleOutputReporter, FileOutputReporter and KafkaOutputReporter bundled in it. If you want to bundle the custom reporters like RedisOutputReporter or InfluxDBOutputReporter in the jar file then provide the maven profile id for that reporter in the build command. For example to build a jar file with RedisOutputReporter, you can execute `mvn -P redis clean package` command. Please check the pom.xml file for available custom reporters and their profile ids. 

## Example to Run with Spark Application

You could upload jvm-profiler jar file to HDFS so the Spark application executors could access it. Then add configuration like following when launching Spark application:

```
--conf spark.jars=hdfs://hdfs_url/lib/jvm-profiler-1.0.0.jar
--conf spark.executor.extraJavaOptions=-javaagent:jvm-profiler-1.0.0.jar
```

## Example to Run with Java Application

Following command will start the example application with the profiler agent attached, which will report metrics to the console output:
```
java -javaagent:target/jvm-profiler-1.0.0.jar=reporter=com.uber.profiling.reporters.ConsoleOutputReporter,tag=mytag,metricInterval=5000,durationProfiling=com.uber.profiling.examples.HelloWorldApplication.publicSleepMethod,argumentProfiling=com.uber.profiling.examples.HelloWorldApplication.publicSleepMethod.1,sampleInterval=100 -cp target/jvm-profiler-1.0.0.jar com.uber.profiling.examples.HelloWorldApplication
```

## Example to Run with Executable Jar

Use following command to run jvm profiler with executable jar application.
```
java -javaagent:/opt/jvm-profiler/target/jvm-profiler-1.0.0.jar=reporter=com.uber.profiling.reporters.ConsoleOutputReporter,metricInterval=5000,durationProfiling=foo.bar.FooAppication.barMethod,sampleInterval=5000 -jar foo-application.jar
```

## Example to Run with Tomcat

Set the jvm profiler in CATALINA_OPTS before starting the tomcat server. Check logs/catalina.out file for metrics.
```
export CATALINA_OPTS="$CATALINA_OPTS -javaagent:/opt/jvm-profiler/target/jvm-profiler-1.0.0.jar=reporter=com.uber.profiling.reporters.ConsoleOutputReporter,metricInterval=5000,durationProfiling=foo.bar.FooController.barMethod,sampleInterval=5000"
```

## Example to Run with Spring Boot Maven Plugin

Use following command to use jvm profiler with Spring Boot 2.x. For Spring Boot 1.x use `-Drun.arguments` instead of `-Dspring-boot.run.jvmArguments` in following command.
```
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-javaagent:/opt/jvm-profiler/target/jvm-profiler-1.0.0.jar=reporter=com.uber.profiling.reporters.ConsoleOutputReporter,metricInterval=5000,durationProfiling=foo.bar.FooController.barMethod,sampleInterval=5000"
```

## Send Metrics to Kafka

Uber JVM Profiler supports sending metrics to Kafka. For example,

```
java -javaagent:target/jvm-profiler-1.0.0.jar=reporter=com.uber.profiling.reporters.KafkaOutputReporter,metricInterval=5000,brokerList=localhost:9092,topicPrefix=profiler_ -cp target/jvm-profiler-1.0.0.jar com.uber.profiling.examples.HelloWorldApplication
```
It will send metrics to Kafka topic profiler_CpuAndMemory. See bottom of this document for an example of the metrics.

## More Details

See [JVM Profiler Blog Post](https://eng.uber.com/jvm-profiler/).

## Feature List

Uber JVM Profiler supports following features:

1. Debug memory usage for all your spark application executors, including java heap memory, non-heap memory, native memory (VmRSS, VmHWM), memory pool, and buffer pool (directed/mapped buffer).

2. Debug CPU usage, Garbage Collection time for all spark executors.

3. Debug arbitrary java class methods (how many times they run, how much duration they spend). We call it Duration Profiling.

4. Debug arbitrary java class method call and trace it argument value. We call it Argument Profiling.

5. Do Stacktrack Profiling and generate flamegraph to visualize CPU time spent for the spark application.

6. Debug IO metrics (disk read/write bytes for the application, CPU iowait for the machine).

7. Debug JVM Thread Metrics like Count of Total Threads, Peak Threads, Live/Active Threads and newly Threads.

## Parameter List

The java agent supports following parameters, which could be used in Java command line like "-javaagent:agent_jar_file.jar=param1=value1,param2=value2":

- reporter: class name for the reporter, e.g. com.uber.profiling.reporters.ConsoleOutputReporter, or com.uber.profiling.reporters.KafkaOutputReporter, which are already implemented in the code. You could implement your own reporter and set the name here.

- configProvider: class name for the config provider, e.g. com.uber.profiling.YamlConfigProvider, which are already implemented in the code. You could implement your own config provider and set the name here.

- configFile: config file path to be used by YamlConfigProvider (if configProvider is set to com.uber.profiling.YamlConfigProvider). This could be a local file path or HTTP URL.

- tag: plain text string which will be reported together with the metrics.

- metricInterval: how frequent to collect and report the metrics, in milliseconds.

- durationProfiling: configure to profile specific class and method, e.g. com.uber.profiling.examples.HelloWorldApplication.publicSleepMethod. It also support wildcard (\*) for method name, e.g. com.uber.profiling.examples.HelloWorldApplication.*.

- argumentProfiling: configure to profile specific method argument, e.g. com.uber.profiling.examples.HelloWorldApplication.publicSleepMethod.1 (".1" means getting value for the first argument and sending out in the reporter).

- sampleInterval: frequency (milliseconds) to do stacktrace sampling, if this value is not set or zero, the profiler will not do stacktrace sampling.

- ioProfiling: whether to profile IO metrics, could be true or false.

- brokerList: broker list if using com.uber.profiling.reporters.KafkaOutputReporter.

- topicPrefix: topic prefix if using com.uber.profiling.reporters.KafkaOutputReporter. KafkaOutputReporter will send metrics to multiple topics with this value as the prefix for topic names.

- outputDir: output directory if using com.uber.profiling.reporters.FileOutputReporter. FileOutputReporter will write metrics into this directory.

## YAML Config File

The parameters could be provided as arguments in java command, or in a YAML config file if you use configProvider=com.uber.profiling.YamlConfigProvider. Following is an example of the YAML config file:

```
reporter: com.uber.profiling.reporters.ConsoleOutputReporter
metricInterval: 5000
```

## Metrics Example

Following is an example of CPU and Memory metrics when using ConsoleOutputReporter or KafkaOutputReporter:

```json
{
	"nonHeapMemoryTotalUsed": 11890584.0,
	"bufferPools": [
		{
			"totalCapacity": 0,
			"name": "direct",
			"count": 0,
			"memoryUsed": 0
		},
		{
			"totalCapacity": 0,
			"name": "mapped",
			"count": 0,
			"memoryUsed": 0
		}
	],
	"heapMemoryTotalUsed": 24330736.0,
	"epochMillis": 1515627003374,
	"nonHeapMemoryCommitted": 13565952.0,
	"heapMemoryCommitted": 257425408.0,
	"memoryPools": [
		{
			"peakUsageMax": 251658240,
			"usageMax": 251658240,
			"peakUsageUsed": 1194496,
			"name": "Code Cache",
			"peakUsageCommitted": 2555904,
			"usageUsed": 1173504,
			"type": "Non-heap memory",
			"usageCommitted": 2555904
		},
		{
			"peakUsageMax": -1,
			"usageMax": -1,
			"peakUsageUsed": 9622920,
			"name": "Metaspace",
			"peakUsageCommitted": 9830400,
			"usageUsed": 9622920,
			"type": "Non-heap memory",
			"usageCommitted": 9830400
		},
		{
			"peakUsageMax": 1073741824,
			"usageMax": 1073741824,
			"peakUsageUsed": 1094160,
			"name": "Compressed Class Space",
			"peakUsageCommitted": 1179648,
			"usageUsed": 1094160,
			"type": "Non-heap memory",
			"usageCommitted": 1179648
		},
		{
			"peakUsageMax": 1409286144,
			"usageMax": 1409286144,
			"peakUsageUsed": 24330736,
			"name": "PS Eden Space",
			"peakUsageCommitted": 67108864,
			"usageUsed": 24330736,
			"type": "Heap memory",
			"usageCommitted": 67108864
		},
		{
			"peakUsageMax": 11010048,
			"usageMax": 11010048,
			"peakUsageUsed": 0,
			"name": "PS Survivor Space",
			"peakUsageCommitted": 11010048,
			"usageUsed": 0,
			"type": "Heap memory",
			"usageCommitted": 11010048
		},
		{
			"peakUsageMax": 2863661056,
			"usageMax": 2863661056,
			"peakUsageUsed": 0,
			"name": "PS Old Gen",
			"peakUsageCommitted": 179306496,
			"usageUsed": 0,
			"type": "Heap memory",
			"usageCommitted": 179306496
		}
	],
	"processCpuLoad": 0.0008024004394748531,
	"systemCpuLoad": 0.23138430784607697,
	"processCpuTime": 496918000,
	"appId": null,
	"name": "24103@machine01",
	"host": "machine01",
	"processUuid": "3c2ec835-749d-45ea-a7ec-e4b9fe17c23a",
	"tag": "mytag",
	"gc": [
		{
			"collectionTime": 0,
			"name": "PS Scavenge",
			"collectionCount": 0
		},
		{
			"collectionTime": 0,
			"name": "PS MarkSweep",
			"collectionCount": 0
		}
	]
}
```

## Metric Details

A list of all metrics and information corresponding to them can be found [here](metricDetails.md).

## Generate flamegraph of Stacktrack Profiling result

We can take the output of Stacktrack Profiling to generate flamegraph to visualize CPU time. Using the Python script `stackcollapse.py`, following command will collapse Stacktrack Profiling json output file to the input file format for generating flamegraph. The script `flamegraph.pl` can be found at [FlameGraph](https://github.com/brendangregg/FlameGraph).

```
python stackcollapse.py -i Stacktrace.json > Stacktrace.folded
flamegraph.pl Stacktrace.folded > Stacktrace.svg
```

Note that it is required to enable stacktrace sampling, in order to generate flamegraph. To enable it, please set `sampleInterval` parameter. If it is not set or zero, the profiler will not do stacktrace sampling.
