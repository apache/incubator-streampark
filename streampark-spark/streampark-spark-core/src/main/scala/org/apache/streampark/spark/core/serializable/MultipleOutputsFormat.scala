/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.streampark.spark.core.serializable

import org.apache.hadoop.io.{DataInputBuffer, NullWritable}
import org.apache.hadoop.mapred.RawKeyValueIterator
import org.apache.hadoop.mapreduce.{Job, _}
import org.apache.hadoop.mapreduce.counters.GenericCounter
import org.apache.hadoop.mapreduce.lib.output.{LazyOutputFormat, MultipleOutputs}
import org.apache.hadoop.mapreduce.task.ReduceContextImpl
import org.apache.hadoop.mapreduce.task.TaskAttemptContextImpl.DummyReporter
import org.apache.hadoop.util.Progress

object MultipleOutputsFormat {
  // Type inference fails with this inlined in constructor parameters
  private def defaultMultipleOutputsMaker[K, V](
      io: TaskInputOutputContext[_, _, K, V]): MultipleOutputer[K, V] =
    new MultipleOutputs[K, V](io)
}

/**
 * Use this abstract class to create multiple outputs of OutputFormat. The output format requires a
 * two-part key (outputPath and actualKey), and the outputPath will be used to output into different
 * directories ('/' separated by filenames).
 *
 * @param outputFormat
 *   OutputFormat
 * @param multipleOutputsMaker
 *   Methods that implement the MultipleOutputer trait
 * @tparam K
 *   Basic OutputFormat's key type
 * @tparam V
 *   Basic OutputFormat's value type
 */
abstract class MultipleOutputsFormat[K, V](
    outputFormat: OutputFormat[K, V],
    multipleOutputsMaker: TaskInputOutputContext[_, _, K, V] => MultipleOutputer[K, V] =
      (r: TaskInputOutputContext[_, _, K, V]) =>
        MultipleOutputsFormat.defaultMultipleOutputsMaker[K, V](r))
  extends OutputFormat[(String, K), V] {

  /**
   * Check for validity of the output-specification for the job.
   *
   * <p>This is to validate the output specification for the job when it is a job is submitted.
   * Typically checks that it does not already exist, throwing an exception when it already exists,
   * so that output is not overwritten.</p>
   *
   * @param context
   *   information about the job
   * @throws IOException
   *   when output should not be attempted
   */
  override def checkOutputSpecs(context: JobContext): Unit =
    outputFormat.checkOutputSpecs(context)

  /**
   * Get the output committer for this output format. This is responsible for ensuring the output is
   * committed correctly.
   *
   * @param context
   *   the task context
   * @return
   *   an output committer
   * @throws IOException
   * @throws InterruptedException
   */
  override def getOutputCommitter(context: TaskAttemptContext): OutputCommitter = outputFormat
    .getOutputCommitter(context)

  /**
   * Get the {@link RecordWriter} for the given task.
   *
   * @param context
   *   the information about the current task.
   * @return
   *   a { @link RecordWriter} to write the output for the job.
   * @throws IOException
   */
  override def getRecordWriter(context: TaskAttemptContext): RecordWriter[(String, K), V] =
    new RecordWriter[(String, K), V] {

      val job: Job = Job.getInstance(context.getConfiguration)
      LazyOutputFormat.setOutputFormatClass(job, outputFormat.getClass)
      // of Spark's saveAs*Hadoop* methods
      val ioContext = new ReduceContextImpl(
        job.getConfiguration,
        context.getTaskAttemptID,
        new DummyIterator,
        new GenericCounter,
        new GenericCounter,
        new DummyRecordWriter,
        new DummyOutputCommitter,
        new DummyReporter,
        null,
        classOf[NullWritable],
        classOf[NullWritable])

      val multipleOutputs: MultipleOutputer[K, V] = multipleOutputsMaker(ioContext)

      /**
       * Writes a keys/value pair.
       *
       * @param keys
       *   the key to write.
       * @param value
       *   the value to write.
       * @throws IOException
       */
      override def write(keys: (String, K), value: V): Unit = {
        keys match {
          case (path, key) =>
            multipleOutputs.write(key, value, path)
        }
      }

      override def close(context: TaskAttemptContext): Unit =
        multipleOutputs.close()
    }

  private class DummyOutputCommitter extends OutputCommitter {
    override def setupJob(jobContext: JobContext): Unit = ()

    override def needsTaskCommit(taskContext: TaskAttemptContext): Boolean =
      false

    override def setupTask(taskContext: TaskAttemptContext): Unit = ()

    override def commitTask(taskContext: TaskAttemptContext): Unit = ()

    override def abortTask(taskContext: TaskAttemptContext): Unit = ()
  }

  private class DummyRecordWriter extends RecordWriter[K, V] {
    override def write(key: K, value: V): Unit = ()

    override def close(context: TaskAttemptContext): Unit = ()
  }

  private class DummyIterator extends RawKeyValueIterator {
    override def getKey: DataInputBuffer = null

    override def getValue: DataInputBuffer = null

    override def getProgress: Progress = null

    override def close(): Unit = ()

    override def next: Boolean = true
  }

}
