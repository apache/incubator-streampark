package com.streamxhub.flink.monitor.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wuwenze.poi.annotation.Excel;
import lombok.Data;

import java.io.File;
import java.io.Serializable;
import java.util.Date;

/**
 * @author benjobs
 */
@Data
@TableName("t_flink_project")
@Excel("flink项目实体")
public class Project implements Serializable {
    @TableId(value = "ID", type = IdType.INPUT)
    private String id;

    private String name;

    private String home;

    private String path;

    private Date date;

    private Long size;

    /**
     *  1:jar
     *  2:project
     */
    private Integer type;

    private transient String dateFrom;
    private transient String dateTo;

    public String getWorkspace() {
        return this.home + File.separator + this.name;
    }


}
