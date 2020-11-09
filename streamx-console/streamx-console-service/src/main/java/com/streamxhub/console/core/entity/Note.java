package com.streamxhub.console.core.entity;

import com.wuwenze.poi.annotation.Excel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @author benjobs
 */
@Data
@Excel("flink应用实体")
@Slf4j
public class Note {
    private String jobName;
    private String env;
    private String sourceCode;

}
