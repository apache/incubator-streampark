package com.streamxhub.flink.monitor.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.streamxhub.common.conf.ConfigConst;
import com.wuwenze.poi.annotation.Excel;
import lombok.Data;
/**
 * @author benjobs
 */
@Data
@TableName("t_app_backup")
@Excel("app备份实体")
public class ApplicationBackUp {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private Long appId;
    private String path;
    private String description;
    private Long timeStamp;

    public ApplicationBackUp(Application application) {
        this.appId = application.getId();
        this.description = application.getBackUpDescription();
        this.timeStamp = System.currentTimeMillis();
        this.path = ConfigConst.APP_HISTORY()
                .concat("/")
                .concat(application.getId().toString())
                .concat("/")
                .concat(timeStamp.toString());
    }

}
