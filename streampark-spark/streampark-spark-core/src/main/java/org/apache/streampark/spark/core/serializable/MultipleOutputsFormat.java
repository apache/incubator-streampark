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

import org.apache.hadoop.io.DataInputBuffer;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapred.RawKeyValueIterator;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.OutputCommitter;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.counters.GenericCounter;
import org.apache.hadoop.mapreduce.lib.output.LazyOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;
import org.apache.hadoop.mapreduce.task.ReduceContextImpl;
import org.apache.hadoop.mapreduce.task.TaskAttemptContextImpl;
import org.apache.hadoop.util.Progress;

import java.io.IOException;

/** Abstract OutputFormat for multiple output paths. */
public abstract class MultipleOutputsFormat<K, V> extends OutputFormat<scala.Tuple2<String, K>, V> {

    private final OutputFormat<K, V> outputFormat;
    private final MultipleOutputerFactory<K, V> multipleOutputsMaker;

    @FunctionalInterface
    public interface MultipleOutputerFactory<K, V> {
        @SuppressWarnings("rawtypes")
        MultipleOutputer<K, V> create(ReduceContextImpl ioContext) throws Exception;
    }

    protected MultipleOutputsFormat(OutputFormat<K, V> outputFormat) {
        this(
                outputFormat,
                ioContext -> new MultipleOutputer.PlainMultipleOutputer<>(new MultipleOutputs<>(ioContext)));
    }

    protected MultipleOutputsFormat(
            OutputFormat<K, V> outputFormat, MultipleOutputerFactory<K, V> multipleOutputsMaker) {
        this.outputFormat = outputFormat;
        this.multipleOutputsMaker = multipleOutputsMaker;
    }

    @Override
    public void checkOutputSpecs(JobContext context) throws IOException, InterruptedException {
        outputFormat.checkOutputSpecs(context);
    }

    @Override
    public OutputCommitter getOutputCommitter(TaskAttemptContext context)
            throws IOException, InterruptedException {
        return outputFormat.getOutputCommitter(context);
    }

    @Override
    public RecordWriter<scala.Tuple2<String, K>, V> getRecordWriter(TaskAttemptContext context)
            throws IOException {
        return new RecordWriter<scala.Tuple2<String, K>, V>() {
            private final MultipleOutputer<K, V> multipleOutputs;

            {
                try {
                    Job job = Job.getInstance(context.getConfiguration());
                    LazyOutputFormat.setOutputFormatClass(job, outputFormat.getClass());
                    @SuppressWarnings({"rawtypes", "unchecked"})
                    ReduceContextImpl ioContext =
                            new ReduceContextImpl(
                                    job.getConfiguration(),
                                    context.getTaskAttemptID(),
                                    new DummyIterator(),
                                    new GenericCounter(),
                                    new GenericCounter(),
                                    new DummyRecordWriter(),
                                    new DummyOutputCommitter(),
                                    new TaskAttemptContextImpl.DummyReporter(),
                                    null,
                                    NullWritable.class,
                                    NullWritable.class);
                    multipleOutputs = multipleOutputsMaker.create(ioContext);
                } catch (Exception e) {
                    throw new IOException(e);
                }
            }

            @Override
            public void write(scala.Tuple2<String, K> keys, V value) throws IOException {
                try {
                    multipleOutputs.write(keys._2(), value, keys._1());
                } catch (Exception e) {
                    throw new IOException(e);
                }
            }

            @Override
            public void close(TaskAttemptContext context) throws IOException {
                try {
                    multipleOutputs.close();
                } catch (Exception e) {
                    throw new IOException(e);
                }
            }
        };
    }

    private static class DummyOutputCommitter extends OutputCommitter {
        @Override
        public void setupJob(JobContext jobContext) throws IOException {}

        @Override
        public boolean needsTaskCommit(TaskAttemptContext taskContext) throws IOException {
            return false;
        }

        @Override
        public void setupTask(TaskAttemptContext taskContext) throws IOException {}

        @Override
        public void commitTask(TaskAttemptContext taskContext) throws IOException {}

        @Override
        public void abortTask(TaskAttemptContext taskContext) throws IOException {}
    }

    private static class DummyRecordWriter<K, V> extends RecordWriter<K, V> {
        @Override
        public void write(K key, V value) throws IOException {}

        @Override
        public void close(TaskAttemptContext context) throws IOException {}
    }

    private static class DummyIterator implements RawKeyValueIterator {
        @Override
        public DataInputBuffer getKey() throws IOException {
            return null;
        }

        @Override
        public DataInputBuffer getValue() throws IOException {
            return null;
        }

        @Override
        public Progress getProgress() {
            return null;
        }

        @Override
        public void close() throws IOException {}

        @Override
        public boolean next() throws IOException {
            return true;
        }
    }
}
