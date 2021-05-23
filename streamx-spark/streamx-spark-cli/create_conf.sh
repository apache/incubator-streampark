#!/usr/bin/env bash
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

#提交脚本run.sh需要的几个配置
function user_run_params(){
	local Lprefix="spark.run"
	_p "必须设置,执行class的全包名称" "main" " "
	_p "必须设置,包含main class的jar包名称\njar文件必须包含在lib.path当中" "main.jar" " "
	_p "提供给执行class的命令行参数,多个参数之间用逗号隔开,参数中不能包含空格等空白符\nEx:param1,param2,.." \
	"self.params" " "
	_p "用户代码依赖jar包的所在目录\n可以是绝对路径,也可以是相对此配置文件的相对路径,相对路径会自动补全" "lib.path" " "
}

#spark任务提交需要的几个基础配置
function spark_run_params(){
	_p "执行集群设置,不用设置,一般使用YARN" "master" "yarn"
	_p "YARN部署模式\ndefault=cluster" "submit.deployMode" "cluster"
	_p "spark-streaming每个批次间隔时间\ndefault=300" "batch.duration" "300"
	_p "spark on yarn的任务提交队列\ndefault=defalut" "yarn.queue" "default"
	_p "spark 任务名称配置,建议保持任务名称全局唯一\n这样可以在设计任务失败的时候根据名称做一些唯一处理\n不设置使用类全名.App" \
	"app.name" ""
	_p "spark网络序列化方式,默认是JavaSerializer,可针对所有类型但速度较慢\n这里使用推荐的Kryo方式\nkafka-0.10必须使用此方式" \
	"serializer" "org.apache.spark.serializer.KryoSerializer"

	_p "++++++++++++++++++++++Driver节点相关配置+++++++++++++++++++++++++++"
	local Lprefix="spark.driver"
	_p "Driver节点使用内存大小设置\ndefault=512MB" "memory" "512MB"
	_p "Driver节点使用的cpu个数设置\ndefault=1" "cores" "1"
	_p "Driver节点构建时spark-jar和user-jar冲突时优先使用用户提供的,这是一个实验性质的参数只对cluster模式有效\ndefault=false" \
	"userClassPathFirst" "false"

	_p "++++++++++++++++++++++Executor节点相关配置+++++++++++++++++++++++++"
	Lprefix="spark.executor"
	_p "Executor个数设置\ndefault=1" "instances" "1"
	_p "Executor使用cpu个数设置\ndefault=1" "cores" "1"
	_p "Executor使用内存大小设置\ndefault=512MB" "memory" "512MB"
	_p "同driver节点配置作用相同,但是是针对executor的\ndefault=false" "userClassPathFirst" "false"
}

#spark 任务动态资源分配的配置
function spark_dynamic_params(){
	_p "++++++++++++++++++++++++Executor动态分配相关配置++++++++++++++++++++"
	local Lprefix="spark.shuffle.service"
	_p "Executor动态分配的前置服务\ndefault=false" "enabled" "true"
	_p "服务对应的端口,此端口服务是配置在yarn-site中的,由NodeManager服务加载启动\ndefault=7337" "port" "7337"

	Lprefix="spark.dynamicAllocation"
	_p "配置是否启用资源动态分配,此动态分配是针对executor的,需要yarn集群配置支持动态分配\ndefault=false" \
	"enabled" "true"
	_p "释放空闲的executor的时间\ndefault=60s" "executorIdleTimeout" "60s"
	_p "有缓存的executor空闲释放时间\ndefault=infinity(默认不释放)" "cachedExecutorIdleTimeout" "-1"
	_p "初始化executor的个数,如果设置spark.executor.instances谁小用谁\ndefault=minExecutors(不设置使用此项配置值)" \
	"initialExecutors" "1"
	_p "executor动态分配可分配最大数量\ndefault=infinity" "maxExecutors" "60"
	_p "executor动态收缩的最小数量\ndefault=0" "minExecutors" "1"
	_p "批次调度延迟多长时间开始增加executor\ndefault=1s" "schedulerBacklogTimeout" "1s"
	_p "同上,但是是针对之后的请求\ndefault=SchedulerBacklogTimeout(不设置使用此项配置值)" \
	"sustainedSchedulerBacklogTimeout" "1s"
}

#消费kafka需要的基础配置
function streamx_spark_source_kafka(){
	create_notes "\nStreamX-Spark Kafka Source\nbase config\n"
	_p "spark.source.kafka.consume后面的配置是标准kafka配置"
	local Lprefix="spark.source.kafka.consume"
	_p "kafka消费的topics配置,可以配置多个,每个topic之间用逗号[,]隔开\ndefault=" "topics" ""
	_p "kafka consumer的group id.\ndefault=kafka.consumer.001" "group.id" "kafka.consumer.001"
	_p "kafka集群的主机和端口号,可以配置多个,每个主机之间用逗号[,]隔开\ndefault=" "bootstrap.servers" ""
	_p "第一次消费kafka topic的时候指定从什么位置消费
		有两个可选值latest[最新位置],earliest[最早位置]\ndefault=earliest" "auto.offset.reset" "earliest"
	_p "spark.source.kafka" "spark消费kafka的时候如何管理offset
		这里可选的值有三种hbase,redis,kafka每种值对应一种存储方式\ndefault=kafka" "offset.store.type" "kafka"
	_p "自定义spark管理kafka offset的方法,需要指定一个自定义类的名称\nspark.source.kafka.offset.store.class=none"
	_p "新版本kafka使用的key序列化方式\ndefault=java.Serialization" \
		"key.deserializer" "org.apache.kafka.common.serialization.StringDeserializer"
	_p "最新版kafka使用的value序列化方式\ndefault=java.Serialization" \
		"value.deserializer" "org.apache.kafka.common.serialization.StringDeserializer"
	_p "获取一次数据的最大长度,此值的大小需要kafka server端支持\ndefault=10485760" "max.partition.fetch.bytes" "10485760"
	_p "获取一次数据请求的最大等待时间\ndefault=3000" "fetch.max.wait.ms" "3000"
}

function streamx_spark_sink_redis(){
	create_notes "\nStreamX-Spark Redis Sink\nbase config\n"
	_p "StreamX-Spark redis sink需要的几个配置"
	local Lprefix="spark.sink.redis"
	_p "redis主机" "host" ""
	_p "redis端口" "port" "6379"
	_p "redis数据库" "db" "0"
	_p "redis连接超时时间" "timeout" "30"
}

function streamx_spark_sink_influx(){
	create_notes "\nStreamX-Spark InfluxDB Sink\nbase config\n"
	_p "StreamX-Spark influxDB sink需要的几个配置"
	local Lprefix="spark.sink.influxDB"
	_p "influxDB主机" "host" ""
	_p "influxDB端口" "port" "8086"
	_p "influxDB数据库" "db" ""
}

function streamx_spark_congestion_monitor(){
	create_notes "\nStreamX-Spark Monitor\nCongestion base config\n"
	_p "StreamX-Spark 自带的拥堵监控需要的几个参数"
	local Lprefix="spark.monitor.congestion"
	_p "钉钉机器人发送消息的api地址,需要从http开头的全路径" "send.api" ""
	_p "堆积了几个批次之后开始告警,默认是0不告警\ndefault=0" "batch" "0"
	_p "钉钉联系人注册钉钉使用的手机号" "ding.to" ""
	Lprefix="spark.monitor.suicide"
	_p "堆积多少个批次之后kill掉任务,默认是0不kill,配合任务自动重启功能可有效重启堆积任务使恢复\ndefault=0" "batch" "0"
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
streamx_spark_congestion_monitor
streamx_spark_source_kafka
streamx_spark_sink_redis
#streamx_spark_sink_influx
