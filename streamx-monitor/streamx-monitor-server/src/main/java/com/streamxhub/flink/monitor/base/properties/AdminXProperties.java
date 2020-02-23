package com.streamxhub.flink.monitor.base.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "adminx")
public class AdminXProperties {

    private ShiroProperties shiro = new ShiroProperties();

    private boolean openAopLog = true;

}
