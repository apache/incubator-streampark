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

    public String getAppHome() {
        if (this.appHome == null) {
            return null;
        }
        return appHome.endsWith("/") ? appHome : appHome + "/";
    }

    public String getUploadDir() {
        File path = new File(getAppHome() + "upload");
        if (!path.exists()) {
            path.mkdirs();
        }
        return path.getAbsolutePath().concat("/");
    }

    public String getWorkSpace() {
        File path = new File(getAppHome() + "workspace");
        if (!path.exists()) {
            path.mkdirs();
        }
        return path.getAbsolutePath().concat("/");
    }
}
