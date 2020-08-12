package com.streamxhub.flink.monitor.core.metrics.flink;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class JobsOverview {

    private List<Job> jobs;

    @Data
    public static class Job {
        @JsonProperty("jid")
        private String id;
        private String name;
        private String state;
        @JsonProperty("start-time")
        private Long startTime;
        @JsonProperty("end-time")
        private Long endTime;
        private Long duration;
        @JsonProperty("last-modification")
        private Long lastModification;
        private Task tasks;
    }

    @Data
    public static class Task {
        private int total;
        private int created;
        private int scheduled;
        private int deploying;
        private int running;
        private int finished;
        private int canceling;
        private int canceled;
        private int failed;
        private int reconciling;
    }
}

