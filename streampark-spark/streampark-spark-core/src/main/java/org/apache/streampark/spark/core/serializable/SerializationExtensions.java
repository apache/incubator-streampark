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

package org.apache.streampark.spark.core.serializable;

import java.io.IOException;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.mapred.AvroKey;
import org.apache.avro.mapreduce.AvroJob;
import org.apache.avro.mapreduce.AvroKeyInputFormat;
import org.apache.avro.mapreduce.AvroKeyOutputFormat;
import org.apache.avro.specific.SpecificData;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.log4j.Logger;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;

/** Avro serialization helpers for Spark. */
public final class SerializationExtensions {

    private static final Logger LOGGER = Logger.getLogger(SerializationExtensions.class);

    private SerializationExtensions() {}

    public static <T extends GenericRecord> Job avroJob(Class<T> clazz) {
        try {
            return avroJob(clazz, Job.getInstance(new Configuration()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T extends GenericRecord> Job avroJob(Class<T> clazz, Job job) {
        Schema schema = SpecificData.get().getSchema(clazz);
        AvroJob.setInputKeySchema(job, schema);
        AvroJob.setOutputKeySchema(job, schema);
        return job;
    }

    public static boolean isDefined(GenericRecord record, String field) {
        if (record.get(field) != null) {
            return true;
        }
        LOGGER.warn(
                "Expected field '"
                        + field
                        + "' to be defined, but it was not on record of type '"
                        + record.getClass()
                        + "'");
        return false;
    }

    public static <T extends GenericRecord> JavaRDD<T> avroFile(
            SparkContext sparkContext, String path, Class<T> clazz) {
        Job job = avroJob(clazz);
        JavaPairRDD<AvroKey, NullWritable> pairRDD =
                JavaPairRDD.fromJavaRDD(
                        sparkContext
                                .newAPIHadoopFile(
                                        path,
                                        AvroKeyInputFormat.class,
                                        AvroKey.class,
                                        NullWritable.class,
                                        job.getConfiguration())
                                .toJavaRDD());
        return pairRDD.map(tuple -> (T) tuple._1.datum());
    }

    public static <T extends GenericRecord> JavaRDD<T> filterIfUnexpectedNull(
            JavaRDD<T> avroRDD, String... fields) {
        return avroRDD.filter(
                r -> {
                    for (String field : fields) {
                        if (!isDefined(r, field)) {
                            return false;
                        }
                    }
                    return true;
                });
    }

    public static <T extends GenericRecord> void saveAsAvroFile(
            JavaRDD<T> avroRDD, String outputPath, Class<T> clazz) throws Exception {
        Job job = avroJob(clazz);
        JavaPairRDD<AvroKey<T>, NullWritable> output =
                avroRDD.mapToPair(r -> new scala.Tuple2<>(new AvroKey<>(r), NullWritable.get()));
        output.saveAsNewAPIHadoopFile(
                outputPath,
                AvroKey.class,
                NullWritable.class,
                AvroKeyOutputFormat.class,
                job.getConfiguration());
    }
}
