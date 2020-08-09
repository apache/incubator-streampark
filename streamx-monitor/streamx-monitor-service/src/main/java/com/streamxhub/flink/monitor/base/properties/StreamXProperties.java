package com.streamxhub.flink.monitor.base.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Data
@Configuration
@ConfigurationProperties(prefix = "streamx")
public class StreamXProperties {

    private String appHome;

    private String nameService;

}
