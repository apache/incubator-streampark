package com.streamxhub.flink.monitor.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.streamxhub.flink.monitor.base.properties.StreamXProperties;
import com.streamxhub.flink.monitor.base.utils.SpringContextUtil;
import com.wuwenze.poi.annotation.Excel;
import lombok.Data;

import java.io.File;
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
    private String config;
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
    private String module;//应用程序模块
    private String options;
    private String shortOptions;

    private String description;
    private Date createTime;
    private String workspace;
    private transient String userName;
    private transient String projectName;
    private transient String createTimeFrom;
    private transient String createTimeTo;

    public File getAppBase() {
        String localWorkspace = SpringContextUtil.getBean(StreamXProperties.class).getAppHome();
        return new File(localWorkspace.concat("/app/").concat(projectId));
    }
}
