package com.streamxhub.spark.monitor.core.domain;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.Date;

@Data
public class SparkMonitor1 {

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
