package com.streamxhub.flink.core.wrapper

import org.apache.hadoop.hbase.client.{Scan => HScan}

class HBaseScan extends HScan with HBaseQuery with Serializable
