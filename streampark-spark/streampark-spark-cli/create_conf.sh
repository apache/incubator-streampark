#!/usr/bin/env bash

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

test -f /etc/profile && . /etc/profile
test -f $HOME/.bash_profile && . $HOME/.bash_profile
base=$(cd $(dirname $0);pwd)

function __check__(){
	local cmd=${1:-"gawk"}
	if ! which $cmd >/dev/null 2>&1;then
		echo "Please install $cmd" >&2
		exit
	fi
}
function create_notes(){
	local msg=$1
	local col=${2:-55}
	__check__
	gawk -vc=$col 'function as(ll,is,ls){for(i=1;i<ll;i++){ls=ls""is};return ls};
	BEGIN{print as(c,"#")}'
	echo -e "$msg" | awk -vc=$col 'function as(ll,is,ls){for(i=1;i<ll;i++){ls=ls""is};return ls};
	{m=$0;l=(c-length(m)-2)/2;s=as(l-1," ");s1=as(c-length("#"s""m)-1," ");print "#"s""m""s1"#"}'
	gawk -vc=$col -vr=$row 'function as(ll,is,ls){for(i=1;i<ll;i++){ls=ls""is};return ls};
	BEGIN{print as(c,"#")}'
}
function _n(){
	echo -e "$@" | awk '{print "#"$0}'
}
function _p(){
	local ph notes k v
	case $# in
		2)
			ph=${Lprefix:-"spark"};notes="#\n";k=$1;v=$2;;
		3)
			ph=${Lprefix:-"spark"};notes="$1";k=$2;v=$3;;
		4)
			ph=$1;notes="$2";k=$3;v=$4;;
		1)
			_n "$@";echo;return;;
		*)
			echo;return;;
	esac
	echo -e "$(_n $notes)\n${ph}.${k}=${v}\n"
}

# Several configurations required to submit the script run.sh
function user_run_params(){
	local Lprefix="spark.run"
	_p "Must be set to execute the full package name of the class" "main" " "
	_p "Must be set, the name of the jar package containing the main class\njar file must be included in lib.path" "main.jar" " "
	_p "The command line parameters provided to execute the class. Multiple parameters are separated by commas. The parameters cannot contain spaces such as spaces\nEx:param1,param2,.." \
	"self.params" " "
	_p "The directory where the user code depends on the jar package\n can be an absolute path or a relative path relative to this configuration file, and the relative path will be automatically completed" "lib.path" " "
}

# Several basic configurations required for spark task submission
function spark_run_params(){
	_p "Perform cluster settings, do not need to set, generally use YARN" "master" "yarn"
	_p "YARN deployment mode\ndefault=cluster" "submit.deployMode" "cluster"
	_p "spark-streaming interval time per batch\ndefault=300" "batch.duration" "300"
	_p "Task submission queue of spark on yarn\default=default" "yarn.queue" "default"
	_p "Spark task name configuration, it is recommended to keep the task name globally unique\nThis can do some unique processing according to the name when the design task fails\nDo not set the use of the full class name.App" \
	"app.name" ""
	_p "The spark network serialization method, the default is JavaSerializer, which can be used for all types but is slower\nThe recommended Kryo method is used here\nkafka-0.10 must use this method" \
	"serializer" "org.apache.spark.serializer.KryoSerializer"

	_p "++++++++++++++++++++++Driver node related configuration+++++++++++++++++++++++++++"
	local Lprefix="spark.driver"
	_p "Driver node uses memory size setting\ndefault=512MB" "memory" "512MB"
	_p "The number of CPUs used by the Driver node is set\ndefault=1" "cores" "1"
	_p "When the driver node is built, spark-jar and user-jar conflict with user-supplied first. This is an experimental parameter that is only valid for cluster mode\ndefault=false" \
	"userClassPathFirst" "false"

	_p "++++++++++++++++++++++Executor node related configurationExecutor node related configuration+++++++++++++++++++++++++"
	Lprefix="spark.executor"
	_p "Executor number setting\ndefault=1" "instances" "1"
	_p "Executor uses the number of cpu settings\ndefault=1" "cores" "1"
	_p "Executor uses the memory size setting\ndefault=512MB" "memory" "512MB"
	_p "The same as the driver node configuration, but for the executor\ndefault=false" "userClassPathFirst" "false"
}

# Configuration of dynamic resource allocation for spark tasks
function spark_dynamic_params(){
	_p "++++++++++++++++++++++++Executor dynamically allocates related configuration++++++++++++++++++++"
	local Lprefix="spark.shuffle.service"
	_p "Front-end services dynamically allocated by Executor\ndefault=false" "enabled" "true"
	_p "The port corresponding to the service. This port service is configured in yarn-site and loaded and started by the NodeManager service\ndefault=7337" "port" "7337"

	Lprefix="spark.dynamicAllocation"
	_p "Configure whether to enable dynamic resource allocation. This dynamic allocation is for executors and requires yarn cluster configuration to support dynamic allocation\ndefault=false" \
	"enabled" "true"
	_p "Time to release idle executors\ndefault=60s" "executorIdleTimeout" "60s"
	_p "Idle release time of cached executors\ndefault=infinity (not released by default)" "cachedExecutorIdleTimeout" "-1"
	_p "Initialize the number of executors. If spark.executor.instances is set, whoever uses it is smaller\ndefault=minExecutors (do not use this configuration value)" \
	"initialExecutors" "1"
	_p "The maximum number of executors that can be allocated dynamically by executor\ndefault=infinity" "maxExecutors" "60"
	_p "Minimum number of executors to shrink dynamically\ndefault=0" "minExecutors" "1"
	_p "How long the batch scheduling delay starts to increase the executor\ndefault=1s" "schedulerBacklogTimeout" "1s"
	_p "Same as above, but for subsequent requests\ndefault=SchedulerBacklogTimeout (do not use this configuration value)" \
	"sustainedSchedulerBacklogTimeout" "1s"
}

# Basic configuration required to consume kafka
function streampark_spark_source_kafka(){
	create_notes "\nStreamPark-Spark Kafka Source\nbase config\n"
	_p "The configuration behind spark.source.kafka.consume is the standard kafka configuration"
	local Lprefix="spark.source.kafka.consume"
	_p "The configuration of topics consumed by kafka can be configured multiple times.
	Each topic is separated by a comma [,]\ndefault=" "topics" ""
	_p "kafka consumer的group id.\ndefault=kafka.consumer.001" "group.id" "kafka.consumer.001"
	_p "The host and port number of the kafka cluster. Multiple hosts can be configured.
	Each host is separated by a comma [,]\ndefault=" "bootstrap.servers" ""
	_p "When consuming a kafka topic for the first time, specify where to consume from.
	There are two optional values, latest[latest location],
	earliest[earliest location]\ndefault=earliest" "auto.offset.reset" "earliest"
	_p "spark.source.kafka" "How to manage offset when Spark consumes kafka.
	There are three optional values: hbase, redis, and kafka.
	Each value corresponds to a storage method ndefault=kafka" "offset.store.type" "kafka"
	_p "To customize the method of spark management kafka offset,
	you need to specify the name of a custom class\nspark.source.kafka.offset.store.class=none"
	_p "The key serialization method used by the new version of kafka\ndefault=java.Serialization" \
		"key.deserializer" "org.apache.kafka.common.serialization.StringDeserializer"
	_p "The value serialization method used by the latest version of kafka\ndefault=java.Serialization" \
		"value.deserializer" "org.apache.kafka.common.serialization.StringDeserializer"
	_p "Get the maximum length of data at one time,
	the size of this value needs to be supported by the kafka server\ndefault=10485760" "max.partition.fetch.bytes" "10485760"
	_p "Get the maximum waiting time for a data request\ndefault=3000" "fetch.max.wait.ms" "3000"
}

function streampark_spark_sink_redis(){
	create_notes "\nStreamPark-Spark Redis Sink\nbase config\n"
	_p "Several configurations required by StreamPark-Spark redis sink"
	local Lprefix="spark.sink.redis"
	_p "redis host" "host" ""
	_p "redis port" "port" "6379"
	_p "redis database" "db" "0"
	_p "redis connection timeout" "timeout" "30"
}

function streampark_spark_sink_influx(){
	create_notes "\nStreamPark-Spark InfluxDB Sink\nbase config\n"
	_p "Several configurations required by StreamPark-Spark influxDB sink"
	local Lprefix="spark.sink.influxDB"
	_p "influxDB host" "host" ""
	_p "influxDB port" "port" "8086"
	_p "influxDB database" "db" ""
}

function streampark_spark_congestion_monitor(){
	create_notes "\nStreamPark-Spark Monitor\nCongestion base config\n"
	_p "Several parameters required for the congestion monitoring that comes with StreamPark-Spark"
	local Lprefix="spark.monitor.congestion"
	_p "The api address of the message sent by the DingTalk robot requires a full path starting with http" "send.api" ""
	_p "After a few batches are accumulated, the alarm will start. The default is 0, no alarm.\ndefault=0" "batch" "0"
	_p "The mobile phone number used by DingTalk contacts to register with DingTalk" "ding.to" ""
	Lprefix="spark.monitor.suicide"
	_p "How many batches are accumulated to kill the task, the default is 0 not to kill,
	with the automatic task restart function can effectively restart the accumulated task to restore \ndefault=0" "batch" "0"
}

function create_default(){
	create_notes "\nspark process startup.sh\nuser config\n"
	user_run_params
	create_notes "\nspark self config\n"
	spark_run_params
	spark_dynamic_params
	#create_notes "\nspark process\nSource config\n"
	#_p;_p;_p;
	#create_notes "\nspark process\nSink config\n"
	#_p;_p;_p;
}

create_default
streampark_spark_congestion_monitor
streampark_spark_source_kafka
streampark_spark_sink_redis
#streampark_spark_sink_influx
