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

package org.apache.streampark.e2e.core;

import lombok.experimental.UtilityClass;

import java.nio.file.Path;
import java.nio.file.Paths;

@UtilityClass
public final class Constants {
  /** tmp directory path */
  public static final Path HOST_TMP_PATH = Paths.get(System.getProperty("java.io.tmpdir"));

  /** chrome download path in host */
  public static final Path HOST_CHROME_DOWNLOAD_PATH = HOST_TMP_PATH.resolve("download");

  /** chrome download path in selenium/standalone-chrome-debug container */
  public static final String SELENIUM_CONTAINER_CHROME_DOWNLOAD_PATH = "/home/seluser/Downloads";

  /** datagen flink sql for test */
  public static final String TEST_FLINK_SQL =
      "CREATE TABLE datagen (\n"
          + "f_sequence INT,\n"
          + "f_random INT,\n"
          + "f_random_str STRING,\n"
          + "ts AS localtimestamp,\n"
          + "WATERMARK FOR ts AS ts\n"
          + ") WITH (\n"
          + "'connector' = 'datagen',\n"
          + "'rows-per-second'='5',\n"
          + "'fields.f_sequence.kind'='sequence',\n"
          + "'fields.f_sequence.start'='1',\n"
          + "'fields.f_sequence.end'='100',\n"
          + "'fields.f_random.min'='1',\n"
          + "'fields.f_random.max'='100',\n"
          + "'fields.f_random_str.length'='10'\n"
          + ");\n"
          + "\n"
          + "CREATE TABLE print_table (\n"
          + "f_sequence INT,\n"
          + "f_random INT,\n"
          + "f_random_str STRING\n"
          + ") WITH (\n"
          + "'connector' = 'print'\n"
          + ");\n"
          + "\n"
          + "INSERT INTO print_table select f_sequence,f_random,f_random_str from datagen;";
}
