/**
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.common.conf

import org.apache.commons.cli.{Option, Options}

import scala.collection.JavaConversions._

/**
 *
 * copy from flink(1.10.0) sourceCode
 * 适用于flink run下的所有可选参数...
 * 注意flink 1.9.0 开始 -yn 参数就标注给过期,1.10已废弃,如加该参数会报错...
 */
object FlinkRunOption {

  val HELP_OPTION: Option = new Option("h", "help", false, "Show the help message for the CLI Frontend or the action.")

  val JAR_OPTION: Option = new Option("j", "jarfile", true, "Flink program JAR file.")

  val CLASS_OPTION: Option = new Option("c", "class", true, "Class with the program entry point (\"main()\" method). Only needed if the " + "JAR file does not specify the class in its manifest.")

  val CLASSPATH_OPTION: Option = new Option("C", "classpath", true, "Adds a URL to each user code " + "classloader  on all nodes in the cluster. The paths must specify a protocol (e.g. file://) and be " + "accessible on all nodes (e.g. by means of a NFS share). You can use this option multiple " + "times for specifying more than one URL. The protocol must be supported by the " + "{@link java.net.URLClassLoader}.")

  val PARALLELISM_OPTION = new Option("p", "parallelism", true, "The parallelism with which to run the program. Optional flag to override the default value " + "specified in the configuration.")

  /**
   * @deprecated This has no effect anymore, we're keeping it to not break existing bash scripts.
   */
  @deprecated
  val LOGGING_OPTION: Option = new Option("q", "sysoutLogging", false, "If present, " + "suppress logging output to standard out.")

  val DETACHED_OPTION = new Option("d", "detached", false, "If present, runs " + "the job in detached mode")

  val SHUTDOWN_IF_ATTACHED_OPTION = new Option("sae", "shutdownOnAttachedExit", false, "If the job is submitted in attached mode, perform a best-effort cluster shutdown " + "when the CLI is terminated abruptly, e.g., in response to a user interrupt, such as typing Ctrl + C.")

  /**
   * @deprecated use non-prefixed variant { @link #DETACHED_OPTION} for both YARN and non-YARN deployments
   */
  @deprecated val YARN_DETACHED_OPTION = new Option("yd", "yarndetached", false, "If present, runs " + "the job in detached mode (deprecated; use non-YARN specific option instead)")

  val ARGS_OPTION: Option = new Option("a", "arguments", true, "Program arguments. Arguments can also be added without -a, simply as trailing parameters.")

  val ADDRESS_OPTION = new Option("m", "jobmanager", true, "Address of the JobManager (master) to which to connect. " + "Use this flag to connect to a different JobManager than the one specified in the configuration.")

  val SAVEPOINT_PATH_OPTION = new Option("s", "fromSavepoint", true, "Path to a savepoint to restore the job from (for example hdfs:///flink/savepoint-1537).")

  val SAVEPOINT_ALLOW_NON_RESTORED_OPTION = new Option("n", "allowNonRestoredState", false, "Allow to skip savepoint state that cannot be restored. " + "You need to allow this if you removed an operator from your " + "program that was part of the program when the savepoint was triggered.")

  val SAVEPOINT_DISPOSE_OPTION: Option = new Option("d", "dispose", true, "Path of savepoint to dispose.")

  // list specific options
  val RUNNING_OPTION: Option =  new Option("r", "running", false, "Show only running programs and their JobIDs")

  val SCHEDULED_OPTION: Option = new Option("s", "scheduled", false, "Show only scheduled programs and their JobIDs")

  val ALL_OPTION: Option = new Option("a", "all", false, "Show all programs and their JobIDs")

  val ZOOKEEPER_NAMESPACE_OPTION: Option = new Option("z", "zookeeperNamespace", true, "Namespace to create the Zookeeper sub-paths for high availability mode")

  lazy val SAVEPOINT_DIRECTORY: String = {
    val clazz = Class.forName("org.apache.flink.configuration.ConfigOptions")
    val chkOptBuilder = clazz.getMethod("key", classOf[String]).invoke(null, "state.savepoints.dir")
    val option = chkOptBuilder.getClass.getMethod("noDefaultValue").invoke(chkOptBuilder)
    option.getClass.getMethod("withDeprecatedKeys", classOf[Array[String]]).invoke(option, Array("savepoints.state.backend.fs.dir"))
    option.getClass.getMethod("withDescription", classOf[String]).invoke(option, "The default directory for savepoints. Used by the state backends that write savepoints to file systems (MemoryStateBackend, FsStateBackend, RocksDBStateBackend).")
    option.getClass.getMethod("key").invoke(option).toString
  }

  val CANCEL_WITH_SAVEPOINT_OPTION: Option = new Option("s", "withSavepoint", true, "**DEPRECATION WARNING**: " + "Cancelling a job with savepoint is deprecated. Use \"stop\" instead. \n Trigger" + " savepoint and cancel job. The target directory is optional. If no directory is " + s"specified, the configured default directory ($SAVEPOINT_DIRECTORY) is used.")

  val STOP_WITH_SAVEPOINT_PATH = new Option("p", "savepointPath", true, s"Path to the savepoint (for example hdfs:///flink/savepoint-1537).If no directory is specified, the configured default will be used ($SAVEPOINT_DIRECTORY).")

  val STOP_AND_DRAIN = new Option("d", "drain", false, "Send MAX_WATERMARK before taking the savepoint and stopping the pipelne.")

  val PY_OPTION: Option = new Option("py", "python", true, "Python script with the program entry point. " + "The dependent resources can be configured with the `--pyFiles` option.")

  val PYFILES_OPTION: Option = new Option("pyfs", "pyFiles", true, "Attach custom python files for job. " + "These files will be added to the PYTHONPATH of both the local client and the remote python UDF worker. " + "The standard python resource file suffixes such as .py/.egg/.zip or directory are all supported. " + "Comma (',') could be used as the separator to specify multiple files " + "(e.g.: --pyFiles file:///tmp/myresource.zip,hdfs:///$namenode_address/myresource2.zip).")

  val PYMODULE_OPTION: Option = new Option("pym", "pyModule", true, "Python module with the program entry point. " + "This option must be used in conjunction with `--pyFiles`.")

  val PYREQUIREMENTS_OPTION: Option = new Option("pyreq", "pyRequirements", true, "Specify a requirements.txt file which defines the third-party dependencies. " + "These dependencies will be installed and added to the PYTHONPATH of the python UDF worker. " + "A directory which contains the installation packages of these dependencies could be specified " + "optionally. Use '#' as the separator if the optional parameter exists " + "(e.g.: --pyRequirements file:///tmp/requirements.txt#file:///tmp/cached_dir).")

  val PYARCHIVE_OPTION: Option = new Option("pyarch", "pyArchives", true, "Add python archive files for job. The archive files will be extracted to the working directory " + "of python UDF worker. Currently only zip-format is supported. For each archive file, a target directory " + "be specified. If the target directory name is specified, the archive file will be extracted to a " + "name can directory with the specified name. Otherwise, the archive file will be extracted to a " + "directory with the same name of the archive file. The files uploaded via this option are accessible " + "via relative path. '#' could be used as the separator of the archive file path and the target directory " + "name. Comma (',') could be used as the separator to specify multiple archive files. " + "This option can be used to upload the virtual environment, the data files used in Python UDF " + "(e.g.: --pyArchives file:///tmp/py37.zip,file:///tmp/data.zip#data --pyExecutable " + "py37.zip/py37/bin/python). The data files could be accessed in Python UDF, e.g.: " + "f = open('data/data.txt', 'r').")

  val PYEXEC_OPTION: Option = new Option("pyexec", "pyExecutable", true, "Specify the path of the python interpreter used to execute the python UDF worker " + "(e.g.: --pyExecutable /usr/local/bin/python3). " + "The python UDF worker depends on Python 3.5+, Apache Beam (version == 2.15.0), " + "Pip (version >= 7.1.0) and SetupTools (version >= 37.0.0). " + "Please ensure that the specified environment meets the above requirements.")

  HELP_OPTION.setRequired(false)

  JAR_OPTION.setRequired(false)
  JAR_OPTION.setArgName("jarfile")

  CLASS_OPTION.setRequired(false)
  CLASS_OPTION.setArgName("classname")

  CLASSPATH_OPTION.setRequired(false)
  CLASSPATH_OPTION.setArgName("url")

  ADDRESS_OPTION.setRequired(false)
  ADDRESS_OPTION.setArgName("host:port")

  PARALLELISM_OPTION.setRequired(false)
  PARALLELISM_OPTION.setArgName("parallelism")

  LOGGING_OPTION.setRequired(false)
  DETACHED_OPTION.setRequired(false)
  SHUTDOWN_IF_ATTACHED_OPTION.setRequired(false)
  YARN_DETACHED_OPTION.setRequired(false)

  ARGS_OPTION.setRequired(false)
  ARGS_OPTION.setArgName("programArgs")
  ARGS_OPTION.setArgs(Option.UNLIMITED_VALUES)

  RUNNING_OPTION.setRequired(false)
  SCHEDULED_OPTION.setRequired(false)

  SAVEPOINT_PATH_OPTION.setRequired(false)
  SAVEPOINT_PATH_OPTION.setArgName("savepointPath")

  SAVEPOINT_ALLOW_NON_RESTORED_OPTION.setRequired(false)

  ZOOKEEPER_NAMESPACE_OPTION.setRequired(false)
  ZOOKEEPER_NAMESPACE_OPTION.setArgName("zookeeperNamespace")

  CANCEL_WITH_SAVEPOINT_OPTION.setRequired(false)
  CANCEL_WITH_SAVEPOINT_OPTION.setArgName("targetDirectory")
  CANCEL_WITH_SAVEPOINT_OPTION.setOptionalArg(true)

  STOP_WITH_SAVEPOINT_PATH.setRequired(false)
  STOP_WITH_SAVEPOINT_PATH.setArgName("savepointPath")
  STOP_WITH_SAVEPOINT_PATH.setOptionalArg(true)

  STOP_AND_DRAIN.setRequired(false)

  PY_OPTION.setRequired(false)
  PY_OPTION.setArgName("pythonFile")

  PYFILES_OPTION.setRequired(false)
  PYFILES_OPTION.setArgName("pythonFiles")

  PYMODULE_OPTION.setRequired(false)
  PYMODULE_OPTION.setArgName("pythonModule")

  PYREQUIREMENTS_OPTION.setRequired(false)

  PYARCHIVE_OPTION.setRequired(false)

  PYEXEC_OPTION.setRequired(false)

  /**
   * 注意 flink run支持的参数全部在此,-yn(yarncontainer)参数从1.9.0开始已经过时,不推荐使用,从1.10开始已废除.如加上该参数会报错.
   *
   * @return
   */
  def allOptions: Options = {
    val commOptions = getRunCommandOptions
    val yarnOptions = getYARNOptions
    val resultOptions = new Options

    commOptions.getOptions.foreach(resultOptions.addOption)
    yarnOptions.getOptions.filter(x => !resultOptions.hasOption(x.getOpt)).foreach(resultOptions.addOption)
    resultOptions
  }

  def getRunCommandOptions: Options = {
    var options = buildGeneralOptions(new Options)
    options = getProgramSpecificOptions(options)
    options.addOption(SAVEPOINT_PATH_OPTION)
    options.addOption(SAVEPOINT_ALLOW_NON_RESTORED_OPTION)
  }

  def getYARNOptions: Options = {
    val shortPrefix = "y"
    val longPrefix = "yarn"
    //yarn
    // Create the command line options
    val query = new Option(shortPrefix + "q", longPrefix + "query", false, "Display available YARN resources (memory, cores)")
    val applicationId = new Option(shortPrefix + "id", longPrefix + "applicationId", true, "Attach to running YARN session")
    val queue = new Option(shortPrefix + "qu", longPrefix + "queue", true, "Specify YARN queue.")
    val shipPath = new Option(shortPrefix + "t", longPrefix + "ship", true, "Ship files in the specified directory (t for transfer)")
    val flinkJar = new Option(shortPrefix + "j", longPrefix + "jar", true, "Path to Flink jar file")
    val jmMemory = new Option(shortPrefix + "jm", longPrefix + "jobManagerMemory", true, "Memory for JobManager Container with optional unit (default: MB)")
    val tmMemory = new Option(shortPrefix + "tm", longPrefix + "taskManagerMemory", true, "Memory per TaskManager Container with optional unit (default: MB)")
    val slots = new Option(shortPrefix + "s", longPrefix + "slots", true, "Number of slots per TaskManager")
    val dynamicproperties = Option.builder(shortPrefix + "D").argName("property=value").numberOfArgs(2).valueSeparator.desc("use value for given property").build
    val name = new Option(shortPrefix + "nm", longPrefix + "name", true, "Set a custom name for the application on YARN")
    val applicationType = new Option(shortPrefix + "at", longPrefix + "applicationType", true, "Set a custom application type for the application on YARN")
    val zookeeperNamespace = new Option(shortPrefix + "z", longPrefix + "zookeeperNamespace", true, "Namespace to create the Zookeeper sub-paths for high availability mode")
    val nodeLabel = new Option(shortPrefix + "nl", longPrefix + "nodeLabel", true, "Specify YARN node label for the YARN application")
    val help = new Option(shortPrefix + "h", longPrefix + "help", false, "Help for the Yarn session CLI.")

    val TARGET = {
      /**
       * val TARGET = ConfigOptions.key("execution.target")
       * .stringType()
       * .noDefaultValue()
       * .withDescription("The deployment target for the execution, e.g. \"local\" for local execution.")
       */
      val clazz = Class.forName("org.apache.flink.configuration.ConfigOptions")
      val deployOptBuilder = clazz.getMethod("key", classOf[String]).invoke(null, "execution.target")
      val typedConfigOptBuilder = deployOptBuilder.getClass.getMethod("stringType").invoke(deployOptBuilder)
      val option = typedConfigOptBuilder.getClass.getMethod("noDefaultValue").invoke(typedConfigOptBuilder)
      val target = option.getClass.getMethod("withDescription", classOf[String]).invoke(option, "The deployment target for the execution, e.g. \"local\" for local execution.")
      target.getClass.getMethod("key").invoke(target).toString
    }

    val executorOption: Option = new Option("e", "executor", true, "The name of the executor to be used for executing the given job, which is equivalent " + s"to the $TARGET config option. The " + "currently available executors are: exec:getExecutorFactoryNames().")
    /**
     * Dynamic properties allow the user to specify additional configuration values with -D, such as
     * <tt> -Dfs.overwrite-files=true  -Dtaskmanager.memory.network.min=536346624</tt>.
     */
    val dynamicProperties: Option = Option.builder("D").argName("property=value").numberOfArgs(2).valueSeparator('=').desc("Generic configuration options for execution/deployment and for the configured " + "executor. The available options can be found at " + "https://ci.apache.org/projects/flink/flink-docs-stable/ops/config.html").build

    val allOptions = new Options

    allOptions.addOption(ADDRESS_OPTION)
    allOptions.addOption(flinkJar)
    allOptions.addOption(jmMemory)
    allOptions.addOption(tmMemory)
    allOptions.addOption(queue)
    allOptions.addOption(query)
    allOptions.addOption(shipPath)
    allOptions.addOption(slots)
    allOptions.addOption(dynamicproperties)
    allOptions.addOption(DETACHED_OPTION)
    allOptions.addOption(YARN_DETACHED_OPTION)
    allOptions.addOption(name)
    allOptions.addOption(applicationId)
    allOptions.addOption(applicationType)
    allOptions.addOption(zookeeperNamespace)
    allOptions.addOption(nodeLabel)
    allOptions.addOption(help)
    allOptions.addOption(executorOption)
    allOptions.addOption(dynamicProperties)
    allOptions
  }

  def buildGeneralOptions(options: Options): Options = {
    options.addOption(HELP_OPTION)
    // backwards compatibility: ignore verbose flag (-v)
    options.addOption(new Option("v", "verbose", false, "This option is deprecated."))
    options
  }

  def getProgramSpecificOptions(options: Options): Options = {
    options.addOption(JAR_OPTION)
    options.addOption(CLASS_OPTION)
    options.addOption(CLASSPATH_OPTION)
    options.addOption(PARALLELISM_OPTION)
    options.addOption(ARGS_OPTION)
    options.addOption(LOGGING_OPTION)
    options.addOption(DETACHED_OPTION)
    options.addOption(SHUTDOWN_IF_ATTACHED_OPTION)
    options.addOption(YARN_DETACHED_OPTION)
    options.addOption(PY_OPTION)
    options.addOption(PYFILES_OPTION)
    options.addOption(PYMODULE_OPTION)
    options.addOption(PYREQUIREMENTS_OPTION)
    options.addOption(PYARCHIVE_OPTION)
    options.addOption(PYEXEC_OPTION)
    options.addOption(ZOOKEEPER_NAMESPACE_OPTION)
    options
  }

}
