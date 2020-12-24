package com.streamxhub.console.core.metrics.flink;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamxhub.common.util.DeflaterUtils;
import com.streamxhub.console.base.utils.CommonUtil;
import lombok.Data;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * @author benjobs
 */
@Data
public class JvmProfiler implements Serializable {

    @JsonIgnore
    private ObjectMapper mapper = new ObjectMapper();

    private String metric;
    private Long id;
    private String token;
    private String type;
    private String profiler;

    @JsonIgnore
    public Map<String, Object> getMetricsAsMap() throws IOException {
        if (CommonUtil.notEmpty(metric)) {
            String content = DeflaterUtils.unzipString(metric);
            return mapper.readValue(content, Map.class);
        }
        return Collections.EMPTY_MAP;
    }
}
