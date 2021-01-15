### Introduction

[Apache Flink](https://flink.apache.org/) is a framework and distributed processing engine for stateful computations
over unbounded and bounded data streams. This is Flink tutorial for running classical wordcount in both batch and
streaming mode.

There're 3 things you need to do before using flink in StreamX Notebook.

* Download [Flink 1.11](https://flink.apache.org/downloads.html) for scala 2.11 (Only scala-2.11 is supported,
  scala-2.12 is not supported yet in StreamX Notebook), unpack it and set `FLINK_HOME` in flink interpreter setting to
  this location.
* Copy flink-python_2.11–1.11.1.jar from flink opt folder to flink lib folder (it is used by pyflink which is supported)
* If you want to run yarn mode, you need to set `HADOOP_CONF_DIR` in flink interpreter setting. And make sure `hadoop`
  is in your `PATH`, because internally flink will call command `hadoop classpath` and put all the hadoop related jars
  in the classpath of flink interpreter process.

There're 6 sub interpreters in flink interpreter, each is used for different purpose. However they are in the the JVM
and share the same ExecutionEnviroment/StremaExecutionEnvironment/BatchTableEnvironment/StreamTableEnvironment.

* `flink`    - Creates ExecutionEnvironment/StreamExecutionEnvironment/BatchTableEnvironment/StreamTableEnvironment and
  provides a Scala environment
* `pyflink`    - Provides a python environment
* `ipyflink`    - Provides an ipython environment
* `ssql`     - Provides a stream sql environment
* `bsql`    - Provides a batch sql environment
