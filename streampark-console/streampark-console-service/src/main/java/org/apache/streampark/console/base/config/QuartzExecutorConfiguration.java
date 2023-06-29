package org.apache.streampark.console.base.config;

import org.apache.streampark.console.core.quartz.QuartzExecutors;
import org.apache.streampark.console.core.quartz.SchedulerApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzExecutorConfiguration {

    @Bean
    public SchedulerApi schedulerApi() {
        return new QuartzExecutors();
    }
}
