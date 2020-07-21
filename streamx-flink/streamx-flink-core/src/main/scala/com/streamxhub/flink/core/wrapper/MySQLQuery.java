package com.streamxhub.flink.core.wrapper;

import java.io.Serializable;


/**
 * @author benjobs
 */
public class MySQLQuery implements Serializable {
    private String table;
    private String field;
    private String timestamp;
    private String option; // >= 0r > ...
    /**
     * 记录分页数据...
     */
    private Integer offset = 0;
    private Integer rows = 5000; // 默认每页5000条记录

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

    /**
     * 构建下一次的查询条件
     */
    public MySQLQuery nextQuery() {
        //分页和不分页是两套逻辑
        if (isLimit()) {
            if (this.size > this.rows) {
                this.firsePage();
                this.timestamp = this.lastTimestamp;
            } else if (this.size.equals(this.rows)) {
                this.nextPage();
            }
        } else {
            this.timestamp = this.lastTimestamp;
        }
        return this;
    }

    private boolean isLimit() {
        return this.offset != null;
    }

    private void nextPage() {
        //如果查询结果集是空,并且分页的offset大于0
        this.offset += 1;
    }

    public int getPageNo() {
        return this.offset * rows;
    }

    public void firsePage() {
        this.offset = 0;
    }

    public boolean isEmpty() {
        return this.size == 0;
    }

    public String getSQL() {
        if (isLimit()) {
            String format = "select * from %s where %s %s '%s' order by %s asc limit %s,%s ";
            return String.format(format, table, field, option, timestamp, field, getPageNo(), rows);
        }
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

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public Integer getRows() {
        return rows;
    }

    public void setRows(Integer rows) {
        this.rows = rows;
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
