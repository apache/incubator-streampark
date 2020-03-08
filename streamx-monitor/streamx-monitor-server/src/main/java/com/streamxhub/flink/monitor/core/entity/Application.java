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
    /**
     * 创建人
     */
    private Long userId;
    private String configFile;
    /**
     * 仅前端页面显示的任务名称
     */
    private String appName;
    /**
     * 程序在yarn中的名称
     */
    private String yarnName;
    private String appId;
    private Integer state;
    private String args;
    private String config;
    private String description;
    private Date createTime;
    private transient String userName;
    private transient String projectName;
    private transient String createTimeFrom;
    private transient String createTimeTo;


}
