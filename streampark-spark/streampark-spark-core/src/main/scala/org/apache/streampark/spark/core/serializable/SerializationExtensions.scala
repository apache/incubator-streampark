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

import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import org.apache.avro.mapred.AvroKey
import org.apache.avro.mapreduce.{AvroJob, AvroKeyInputFormat, AvroKeyOutputFormat}
import org.apache.avro.specific.SpecificData
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.io.NullWritable
import org.apache.hadoop.mapreduce.Job
import org.apache.log4j.Logger
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import scala.reflect.{classTag, ClassTag}

object SerializationExtensions {

  private val logger = Logger.getLogger(getClass)

  /**
   * Build a AvroJob
   *
   * @param job
   * @tparam T
   * @return
   */
  def avroJob[T <: GenericRecord: ClassTag](
      job: Job = Job.getInstance(new Configuration())): Job = {
    val schema: Schema = SpecificData.get.getSchema(classTag[T].runtimeClass)
    AvroJob.setInputKeySchema(job, schema)
    AvroJob.setOutputKeySchema(job, schema)
    job
  }

  def isDefined(record: GenericRecord, field: String): Boolean = {
    if (record.get(field) != null) return true
    logger.warn(
      s"Expected field '$field' to be defined, but it was not on record of type '${record.getClass}'")
    false
  }

  implicit class AvroRDDSparkContext(val sparkContext: SparkContext) extends AnyVal {

    /**
     * Read the Avro file in the specified path
     *
     * @param path
     * @tparam T
     * @return
     */
    def avroFile[T: ClassTag](path: String): RDD[T] = {
      sparkContext
        .newAPIHadoopFile(
          path,
          classOf[AvroKeyInputFormat[T]],
          classOf[AvroKey[T]],
          classOf[NullWritable],
          avroJob().getConfiguration)
        .map[T](_._1.datum())
    }
  }

  implicit class AvroRDD[T <: GenericRecord: ClassTag](val avroRDD: RDD[T]) {
    def filterIfUnexpectedNull(fields: String*): RDD[T] = {
      avroRDD.filter(r => fields.forall(isDefined(r, _)))
    }

    /** Save as avro file */
    def saveAsAvroFile(outputPath: String): Unit = {
      avroRDD
        .map(r => (new AvroKey[T](r), NullWritable.get))
        .saveAsNewAPIHadoopFile(
          outputPath,
          classOf[AvroKey[T]],
          classOf[NullWritable],
          classOf[AvroKeyOutputFormat[T]],
          avroJob[T]().getConfiguration)
    }

    /**
     * Save the specified avro files according to the Key value
     *
     * @param outputKeyFun
     *   outputKeyFunction
     * @param outputPath
     *   outputPath
     */
    def saveAsMultipleAvroFiles(outputKeyFun: (T) => String, outputPath: String): Unit = {
      avroRDD
        .map(r => ((outputKeyFun(r), new AvroKey(r)), NullWritable.get))
        .saveAsNewAPIHadoopFile(
          outputPath,
          classOf[AvroKey[(String, T)]],
          classOf[NullWritable],
          classOf[MultipleAvroOutputsFormat[T]],
          avroJob[T]().getConfiguration)
    }
  }

}
