package com.streamxhub.spark.core.serializable

import org.apache.hadoop.io.{NullWritable, Text}
import org.apache.hadoop.mapreduce.lib.output.{TextOutputFormat => OutFormat}

/**
  *
  */
class MultipleTextOutputsFormat extends MultipleOutputsFormat(new OutFormat[NullWritable, Text]) {}
