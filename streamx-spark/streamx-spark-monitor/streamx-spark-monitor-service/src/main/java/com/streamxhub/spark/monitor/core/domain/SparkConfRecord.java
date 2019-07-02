package com.streamxhub.spark.monitor.core.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.Date;

@Data
@TableName("t_spark_conf_record")
public class SparkConfRecord {

    @TableId(value = "RECORD_ID", type = IdType.AUTO)
    private Integer recordId;

    private Integer parentId;

    @NotBlank(message = "{required}")
    private String myId;

    @NotBlank(message = "{required}")
    private String appName;

    @NotBlank(message = "{required}")
    private Integer confVersion;

    @NotBlank(message = "{required}")
    private String conf;

    private Date createTime;

    public SparkConfRecord() {
    }

    public SparkConfRecord(String confId, String appName, Integer confVersion, String conf) {
        this.myId = confId;
        this.appName = appName;
        this.confVersion = confVersion;
        this.conf = conf;
        this.createTime = new Date();
    }
}
