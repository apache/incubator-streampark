package com.streamxhub.flink.monitor.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.wuwenze.poi.annotation.Excel;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("t_flink_app")
@Excel("flink应用实体")
public class Application implements Serializable {
    /**
     * what fuck。。。
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String projectId;
    private String configFile;
    private String appName;
    private String appId;
    private String state;
    private String args;
    private String config;
    private String description;
    private Date createTime;
    private transient String projectName;
    private transient String createTimeFrom;
    private transient String createTimeTo;


}
