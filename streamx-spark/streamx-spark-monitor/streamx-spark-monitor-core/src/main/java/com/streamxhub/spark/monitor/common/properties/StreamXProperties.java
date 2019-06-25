package com.streamxhub.spark.monitor.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "lark")
public class StreamXProperties {

    private ShiroProperties shiro = new ShiroProperties();

    private boolean openAopLog = true;

}
