package com.streamxhub.spark.monitor.core.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.Date;
import java.util.List;

@Data
@TableName("t_spark_conf")
public class SparkConf {

    @NotBlank(message = "{required}")
    @TableId(value = "MY_ID", type = IdType.INPUT)
    private String myId;

    @NotBlank(message = "{required}")
    private String appName;

    @NotBlank(message = "{required}")
    private Integer confVersion;

    @NotBlank(message = "{required}")
    private String conf;

    private Long confOwner;

    private transient List<SparkConfRecord> history;

    private Date createTime;
    private Date modifyTime;

    public SparkConf(String id, String appName, Integer confVersion, String conf) {
        this.myId = id;
        this.appName = appName;
        this.confVersion = confVersion;
        this.conf = conf;
        this.confOwner = 0L;
    }

    public SparkConf(){}
}
