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

package org.apache.streampark.flink.core;

import java.util.Optional;

/** Operand converters for {@link SqlCommand}. */
final class SqlCommandConverters {

    private SqlCommandConverters() {
    }

    static Optional<String[]> noOperands(String[] groups) {
        if (groups == null) {
            return Optional.empty();
        }
        return Optional.of(new String[0]);
    }

    static Optional<String[]> firstGroup(String[] groups) {
        return Optional.of(new String[]{groups[0]});
    }

    static Optional<String[]> setOperands(String[] groups) {
        if (groups.length < 3) {
            return Optional.empty();
        }
        if (groups[0] == null) {
            return Optional.of(new String[]{cleanUp(groups[0])});
        }
        return Optional.of(new String[]{cleanUp(groups[1]), cleanUp(groups[2])});
    }

    static Optional<String[]> resetAll(String[] groups) {
        if (groups == null) {
            return Optional.empty();
        }
        return Optional.of(new String[]{"ALL"});
    }

    private static String cleanUp(String sql) {
        if (sql == null) {
            return null;
        }
        String trimmed = sql.trim();
        if (trimmed.length() >= 2
            && ((trimmed.startsWith("'") && trimmed.endsWith("'"))
                || (trimmed.startsWith("\"") && trimmed.endsWith("\"")))) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }
}
