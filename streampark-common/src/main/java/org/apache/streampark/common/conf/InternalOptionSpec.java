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

package org.apache.streampark.common.conf;

/**
 * Non-generic view of {@link InternalOption} used where the type parameter is unknown at compile
 * time (e.g. when looking up options by key from a map).
 */
public interface InternalOptionSpec {

    String getKey();

    Object getDefaultValue();

    Class<?> getClassType();

    String getDescription();

    // Scala-compatible accessors
    String key();

    Object defaultValue();

    Class<?> classType();

    String description();
}
