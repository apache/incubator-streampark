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

package org.apache.streampark.flink.connector.jdbc.bean;

import org.apache.streampark.common.util.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Two-phase commit transaction state for JDBC sink. */
public class Transaction implements Serializable {

    private final String transactionId;
    private final List<String> sql;
    private boolean insertMode = true;
    private boolean invoked = false;

    public Transaction() {
        this(Utils.uuid(), new ArrayList<>());
    }

    public Transaction(String transactionId, List<String> sql) {
        this.transactionId = transactionId;
        this.sql = sql;
    }

    public void addSql(String text) {
        sql.add(text);
    }

    public String getTransactionId() {
        return transactionId;
    }

    public List<String> getSql() {
        return sql;
    }

    public boolean isInsertMode() {
        return insertMode;
    }

    public void setInsertMode(boolean insertMode) {
        this.insertMode = insertMode;
    }

    public boolean isInvoked() {
        return invoked;
    }

    public void setInvoked(boolean invoked) {
        this.invoked = invoked;
    }

    @Override
    public String toString() {
        return String.format(
                "(transactionId:%s,size:%d,insertMode:%s,invoked:%s)",
                transactionId, sql.size(), insertMode, invoked);
    }
}
