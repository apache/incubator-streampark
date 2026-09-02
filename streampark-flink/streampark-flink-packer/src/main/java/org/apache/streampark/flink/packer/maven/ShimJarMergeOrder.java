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

package org.apache.streampark.flink.packer.maven;

import java.io.File;
import java.util.Comparator;

/** Controls duplicate class resolution when shading StreamPark shims into a fat jar. */
final class ShimJarMergeOrder {

    private static final String SHIMS_BASE_PREFIX = "streampark-flink-shims-base-";
    private static final String SHIMS_BASE_V2_PREFIX = "streampark-flink-shims-base-v2-";

    static final Comparator<File> COMPARATOR = Comparator.comparingInt(ShimJarMergeOrder::mergeOrder);

    private ShimJarMergeOrder() {
    }

    private static int mergeOrder(File jar) {
        String name = jar.getName();
        if (name.startsWith(SHIMS_BASE_V2_PREFIX)) {
            return 0;
        }
        if (name.startsWith(SHIMS_BASE_PREFIX)) {
            return 1;
        }
        return 2;
    }
}
