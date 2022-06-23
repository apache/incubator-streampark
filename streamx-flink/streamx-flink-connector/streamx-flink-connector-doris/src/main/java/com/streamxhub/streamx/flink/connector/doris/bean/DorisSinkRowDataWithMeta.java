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
