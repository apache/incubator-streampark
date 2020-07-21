package com.streamxhub.flink.core.wrapper;

import java.io.Serializable;


/**
 * @author benjobs
 */
public class MySQLQuery implements Serializable {
    private String table;
    private String field;
    private String timestamp;
    private String option;

    /**
     * 采用fetch模式拉取,每次拉取的大小
     */
    private Integer fetchSize = 2000;
    private Integer size = 0;
    private String lastTimestamp;

    public MySQLQuery() {
    }

    public MySQLQuery(String table, String field, String timestamp, String option) {
        this.table = table;
        this.field = field;
        this.timestamp = timestamp;
        this.option = option;
    }

    public boolean isEmpty() {
        return this.size == 0;
    }

    public String getSQL() {
        String format = "select * from %s where %s %s '%s' order by %s asc";
        return String.format(format, table, field, option, timestamp, field);
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getOption() {
        return option;
    }

    public void setOption(String option) {
        this.option = option;
    }

    public Integer getFetchSize() {
        return fetchSize;
    }

    public void setFetchSize(Integer fetchSize) {
        this.fetchSize = fetchSize;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getLastTimestamp() {
        return lastTimestamp;
    }

    public void setLastTimestamp(String lastTimestamp) {
        this.lastTimestamp = lastTimestamp;
    }
}
