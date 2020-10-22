package com.streamxhub.console.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wuwenze.poi.annotation.Excel;
import lombok.Data;

import java.util.Date;

/**
 * @author benjobs
 */
@Data
@TableName("t_flink_tutorial")
@Excel("使用教程")
public class Tutorial {
    private Long id;
    private String name;
    private Integer type;
    private String content;
    private Date createTime;
}
