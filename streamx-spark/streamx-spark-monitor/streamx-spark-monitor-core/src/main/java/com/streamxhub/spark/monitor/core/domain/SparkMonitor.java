package com.streamxhub.spark.monitor.core.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.Date;

@Data
@TableName("t_spark_monitor")
public class SparkMonitor {

    @NotBlank(message = "{required}")
    private String monitorId;

    @NotBlank(message = "{required}")
    private String appId;

    @NotBlank(message = "{required}")
    private String appName;

    @NotBlank(message = "{required}")
    private String confVersion;

    private Integer status;

    private Date createTime;
    private Date modifyTime;

}
