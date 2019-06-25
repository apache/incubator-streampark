package com.streamxhub.spark.monitor.core.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.Date;

@Data
@TableName("t_spark_conf")
public class SparkConf {

    @NotBlank(message = "{required}")
    @TableId(value = "CONF_ID", type = IdType.INPUT)
    private String confId;

    @NotBlank(message = "{required}")
    private String appName;

    @NotBlank(message = "{required}")
    private String confVersion;

    @NotBlank(message = "{required}")
    private String conf;

    private Date createTime;
    private Date modifyTime;

    public SparkConf(String id, String appName, String confVersion, String conf) {
        this.confId = id;
        this.appName = appName;
        this.confVersion = confVersion;
        this.conf = conf;
    }

    public SparkConf(){}
}
