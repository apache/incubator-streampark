package com.streamxhub.streamx.console.base.converter;

import lombok.extern.slf4j.Slf4j;

import com.streamxhub.streamx.console.base.utils.DateUtil;
import com.wuwenze.poi.convert.WriteConverter;
import com.wuwenze.poi.exception.ExcelKitWriteConverterException;

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
            log.info("时间转换异常", e);
            return "";
        }
    }
}
