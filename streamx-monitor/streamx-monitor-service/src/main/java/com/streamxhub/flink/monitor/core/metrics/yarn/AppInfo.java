package com.streamxhub.flink.monitor.core.metrics.yarn;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;


@Data
public class AppInfo {

    private App app;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class App {
        private String id;
        private String user;
        private String name;
        private String queue;
        private String state;
        private String finalStatus;
        private Float progress;
        private String trackingUI;
        private String trackingUrl;
        private String clusterId;
        private String applicationType;
        private Long startedTime;
        private Long finishedTime;
        private Long elapsedTime;
        private String amContainerLogs;
        private String amHostHttpAddress;
        private String allocatedMB;
        private String allocatedVCores;
        private String reservedMB;
        private String reservedVCores;
        private String runningContainers;
        private Long memorySeconds;
        private Long vcoreSeconds;
        private Long preemptedResourceMB;
        private Long preemptedResourceVCores;
        private Long numNonAMContainerPreempted;
        private Long numAMContainerPreempted;
        private String logAggregationStatus;
    }

}
