package com.streamxhub.spark.monitor.core.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("t_spark_monitor")
public class SparkMonitor {

    @NotBlank(message = "{required}")
    @TableId(value = "MY_ID", type = IdType.INPUT)
    private String myId;

    @NotBlank(message = "{required}")
    private String appId;

    @NotBlank(message = "{required}")
    private String appName;

    @NotBlank(message = "{required}")
    private String confVersion;

    private String trackUrl;

    private String startUp;

    private Integer status;

    private Date createTime;

    private Date modifyTime;

    public SparkMonitor() {
    }

    public void setStatusValue(Status status) {
        this.setStatus(status.getValue());
    }

    public SparkMonitor(String appId, String appName, String confVersion,String startUp) {
        this.appId = appId;
        this.appName = appName;
        this.confVersion = confVersion;
        this.startUp = startUp;
    }

    public enum Status implements Serializable {
        /**
         * 意外终止
         */
        LOST(-1,"意外终止"),
        /**
         * 正常运行
         */
        RUNNING(0, "正常运行"),
        /**
         * 停止
         */
        KILLED(1, "停止"),
        /**
         * 启动中
         */
        STARTING(2, "启动中"),
        /**
         * 启动失败
         */
        START_FAILURE(3, "启动失败"),
        /**
         * 停止中
         */
        KILLING(4, "停止中"),
        /**
         * 停止失败
         */
        KILL_FAILURE(5, "停止失败");

        private Integer value;
        private String description;

        Status(Integer value, String description) {
            this.value = value;
            this.description = description;
        }

        public Integer getValue() {
            return value;
        }

        public void setValue(Integer status) {
            this.value = status;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public static Status getByStatus(Integer status) {
            for (Status value : Status.values()) {
                if (value.getValue().equals(status)) {
                    return value;
                }
            }
            return null;
        }
    }
}
