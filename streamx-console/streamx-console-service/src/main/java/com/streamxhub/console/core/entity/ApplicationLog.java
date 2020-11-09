package com.streamxhub.console.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wuwenze.poi.annotation.Excel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

/**
 * @author benjobs
 */
@Data
@TableName("t_flink_log")
@Excel("flink应用配置")
@Slf4j
public class ApplicationLog {

    private Long id;
    /**
     * appId
     */
    private Long appId;
    /**
     * applicationId
     */
    private String yarnAppId;
    /**
     * 启动状态
     */
    private Boolean success;

    /**
     * 启动时间
     */
    private Date startTime;

    /**
     * 启动失败的异常
     */
    private String exception;

}
