package com.streamxhub.monitor.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.wuwenze.poi.annotation.Excel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @author benjobs
 */

@Data
@TableName("t_flink_config")
@Excel("flink应用配置")
@Slf4j
public class ApplicationConfig {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private Long appId;

    private Boolean actived;

    /**
     * 1)yaml
     * 2)prop
     */
    private Integer format;

    /**
     * 默认版本号:1
     */
    private Integer version = 1;

    private String content;


}
