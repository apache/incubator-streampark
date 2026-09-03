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

package org.apache.streampark.common.configuration;

/** Shared literal values used while resolving StreamPark configuration and application artifacts. */
public final class Constants {

    private Constants() {
    }

    /** Conventional name used when no explicit value is supplied. */
    public static final String DEFAULT = "default";

    /** Canonical StreamPark product identifier. */
    public static final String STREAM_PARK = "streampark";

    /** Plain HTTP URI scheme prefix. */
    public static final String HTTP_SCHEMA = "http://";

    /** TLS-enabled HTTP URI scheme prefix. */
    public static final String HTTPS_SCHEMA = "https://";

    /** Java archive filename suffix. */
    public static final String JAR_SUFFIX = ".jar";

    /** ZIP archive filename suffix. */
    public static final String ZIP_SUFFIX = ".zip";

    /** Python source filename suffix. */
    public static final String PYTHON_SUFFIX = ".py";

    /** Legacy mask used by existing external responses. */
    public static final String DEFAULT_DATAMASK_STRING = "********";

    /** Flink entry class used to launch Python applications. */
    public static final String PYTHON_FLINK_DRIVER_CLASS_NAME = "org.apache.flink.client.python.PythonDriver";

    /** StreamPark entry class used to launch Flink SQL applications. */
    public static final String STREAMPARK_FLINKSQL_CLIENT_CLASS = "org.apache.streampark.flink.cli.SqlClient";

    /** StreamPark entry class used to launch Spark SQL applications. */
    public static final String STREAMPARK_SPARKSQL_CLIENT_CLASS = "org.apache.streampark.spark.cli.SqlClient";

    /** Python executable path inside the packaged virtual environment. */
    public static final String PYTHON_EXECUTABLE = "venv.zip/venv/bin/python3";

}
