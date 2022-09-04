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

package org.apache.streampark.flink.connector.doris.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

/**
 * doris sink buffer
 */
public class DorisSinkBufferEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    public ArrayList<byte[]> getBuffer() {
        return buffer;
    }

    private ArrayList<byte[]> buffer = new ArrayList<>();
    private int batchCount = 0;
    private long batchSize = 0;
    private String label;
    private String database;
    private String table;
    private String labelPrefix;

    public DorisSinkBufferEntry(String database, String table, String labelPrefix) {
        this.database = database;
        this.table = table;
        this.labelPrefix = labelPrefix;
        label = createLabel();
    }

    public void setBuffer(ArrayList<byte[]> buffer) {
        this.buffer = buffer;
    }

    public int getBatchCount() {
        return batchCount;
    }

    public void setBatchCount(int batchCount) {
        this.batchCount = batchCount;
    }

    public long getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(long batchSize) {
        this.batchSize = batchSize;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getLabelPrefix() {
        return labelPrefix;
    }

    public void setLabelPrefix(String labelPrefix) {
        this.labelPrefix = labelPrefix;
    }

    public void addToBuffer(byte[] bytes) {
        incBatchCount();
        incBatchSize(bytes.length);
        buffer.add(bytes);
    }

    private void incBatchSize(long batchSize) {
        this.batchSize += batchSize;
    }

    private void incBatchCount() {
        this.batchCount += 1;
    }

    public synchronized void clear() {
        buffer.clear();
        batchCount = 0;
        batchSize = 0;
        label = createLabel();
    }

    public String reGenerateLabel() {
        return label = createLabel();
    }

    public String createLabel() {
        return String.format("%s_%s_%s", labelPrefix, System.currentTimeMillis(), UUID.randomUUID().toString().replaceAll("-", ""));
    }
}
