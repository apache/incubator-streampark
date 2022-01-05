package com.streamxhub.streamx.console.base.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "streamx.dingding")
public class DingdingProperties {
    private String url;
    private boolean enabled;
    private String secret;
    private String accessToken;
}
