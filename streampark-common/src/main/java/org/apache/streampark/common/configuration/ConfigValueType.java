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

/**
 * Internal conversion and canonical-format contract for one option value type.
 *
 * <p>Implementations must be stateless and thread-safe because {@link ConfigOption} instances are
 * shared constants. Conversion accepts the raw representation retained by {@link Configuration};
 * formatting produces the stable representation exposed by untyped views.
 *
 * @param <T> converted value type
 */
interface ConfigValueType<T> {

    /** Returns the runtime class of converted values. */
    Class<T> valueClass();

    /** Converts a non-null raw configuration value. */
    T convert(Object value);

    /** Formats a converted value for an untyped configuration view. */
    String format(T value);
}
