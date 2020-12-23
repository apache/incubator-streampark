package com.streamxhub.console.core.metrics.flink;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

/**
 * @author benjobs
 */
@Data
public class JvmProfiler implements Serializable {

    @JsonIgnore
    private ObjectMapper mapper = new ObjectMapper();

    private String metric;
    private String id;
    private String token;
    private String type;

    @JsonIgnore
    public Map<String, Object> getMetrics() throws IOException {
        return mapper.readValue(metric, Map.class);
    }
}
