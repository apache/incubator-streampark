package com.streamxhub.spark.monitor.common.converter;

import com.streamxhub.spark.monitor.common.utils.DateUtil;
import com.wuwenze.poi.convert.WriteConverter;
import com.wuwenze.poi.exception.ExcelKitWriteConverterException;
import lombok.extern.slf4j.Slf4j;

/**
 * Execl导出时间类型字段格式化
 */
@Slf4j
public class TimeConverter implements WriteConverter {
    @Override
    public String convert(Object value) throws ExcelKitWriteConverterException {
        try {
            if (value == null) {
                return "";
            } else {
                return DateUtil.formatCSTTime(value.toString(), DateUtil.FULL_TIME_SPLIT_PATTERN);
            }
        } catch (Exception e) {
            log.error("时间转换异常", e);
            return "";
        }
    }
}
