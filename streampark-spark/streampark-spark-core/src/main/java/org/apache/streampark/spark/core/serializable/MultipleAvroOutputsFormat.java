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

import org.apache.avro.generic.GenericContainer;
import org.apache.avro.mapred.AvroKey;
import org.apache.avro.mapreduce.AvroKeyOutputFormat;
import org.apache.avro.mapreduce.AvroMultipleOutputs;
import org.apache.hadoop.io.NullWritable;

/** Multiple Avro output format. */
public class MultipleAvroOutputsFormat<T extends GenericContainer>
        extends MultipleOutputsFormat<AvroKey<T>, NullWritable> {

    public MultipleAvroOutputsFormat() {
        super(
                new AvroKeyOutputFormat<>(),
                ioContext -> new MultipleOutputer.AvroMultipleOutputer<>(new AvroMultipleOutputs(ioContext)));
    }
}
