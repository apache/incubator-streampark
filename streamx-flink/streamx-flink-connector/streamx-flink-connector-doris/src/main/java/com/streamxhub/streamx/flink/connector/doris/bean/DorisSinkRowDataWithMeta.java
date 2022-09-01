/*
 * Copyright 2019 The StreamX Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.flink.connector.doris.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DorisSinkRowDataWithMeta implements Serializable {

    private static final long serialVersionUID = 1L;

    private String table;
    private String database;
    private List<String> dataRows = new ArrayList<>();

    public DorisSinkRowDataWithMeta database(String database) {
        this.database = database;
        return this;
    }

    public DorisSinkRowDataWithMeta table(String table) {
        this.table = table;
        return this;
    }

    public String[] getDataRows() {
        return dataRows.toArray(new String[]{});
    }

    public void addDataRow(String dataRow) {
        this.dataRows.add(dataRow);
    }

    public String getTable() {
        return table;
    }

    public String getDatabase() {
        return database;
    }
}
