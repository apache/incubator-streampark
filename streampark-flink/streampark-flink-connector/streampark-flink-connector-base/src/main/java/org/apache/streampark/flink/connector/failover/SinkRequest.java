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

package org.apache.streampark.flink.connector.failover;

import org.apache.streampark.common.constants.Constants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Buffered sink request with SQL statement grouping. */
public class SinkRequest {

    private static final Logger LOG = LoggerFactory.getLogger(SinkRequest.class);

    private static final Pattern TABLE_REGEXP =
            Pattern.compile(
                    "(?i)(?:insert\\s+into|update|delete)\\s+(\\S+)");
    private static final Pattern INSERT_REGEXP =
            Pattern.compile("(?i)^(.+?)\\s+(?:values?)\\s*(.*)");

    private final List<String> records;
    private int attemptCounter;

    public SinkRequest(List<String> records) {
        this(records, 0);
    }

    public SinkRequest(List<String> records, int attemptCounter) {
        this.records = records;
        this.attemptCounter = attemptCounter;
    }

    public void incrementCounter() {
        attemptCounter += 1;
    }

    public int size() {
        return records.size();
    }

    public List<String> getRecords() {
        return records;
    }

    public int getAttemptCounter() {
        return attemptCounter;
    }

    public List<String> getSqlStatement() {
        List<String> result = new ArrayList<>();
        Map<String, List<String>> prefixMap = new HashMap<>();

        for (String record : records) {
            Matcher valueMatcher = INSERT_REGEXP.matcher(record);
            if (valueMatcher.find()) {
                String prefix = valueMatcher.group(1);
                prefixMap.computeIfAbsent(prefix, k -> new ArrayList<>()).add(valueMatcher.group(2));
            } else {
                LOG.warn("ignore record: {}", record);
            }
        }

        if (!prefixMap.isEmpty()) {
            for (Map.Entry<String, List<String>> entry : prefixMap.entrySet()) {
                result.add(
                        String.format(
                                "%s VALUES %s",
                                entry.getKey(), String.join(",", entry.getValue())));
            }
        }

        LOG.debug("script to commit: {}", String.join(Constants.SEMICOLON, result));
        return result;
    }

    public String getTable() {
        if (records.isEmpty()) {
            return null;
        }
        Matcher matcher = TABLE_REGEXP.matcher(records.get(0));
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
