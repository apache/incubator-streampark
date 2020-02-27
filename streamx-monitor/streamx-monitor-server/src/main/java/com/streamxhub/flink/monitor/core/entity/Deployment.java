package com.streamxhub.flink.monitor.core.entity;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.Serializable;
import java.util.concurrent.*;

@Data
@Slf4j
public class Deployment implements Serializable {

    private String basePath;
    private String confPath;
    private String binPath;
    private String logPath;
    private String tempPath;
    private String startup;
    private String stop;
    private String conf;
    private String args;

    private ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("Flink-StartUp-%d").build();

    public Deployment(Project project, Application application) {
        this.basePath = project.getHome() + File.separator + project.getName();
        this.confPath = this.basePath + "/conf";
        this.binPath = this.basePath + "/bin";
        this.logPath = this.basePath + "/logs";
        this.tempPath = this.basePath + "/temp";
        this.startup = this.binPath + "/startup.sh";
        this.stop = this.binPath + "/stop.sh";
        this.conf = application.getConfigFile();
    }

    public void startUp() {
        ExecutorService threadPoolExecutor = Executors.newSingleThreadExecutor(namedThreadFactory);
        threadPoolExecutor.execute(() -> {
            String command = String.format("bash +x %s %s %s", this.startup, this.conf, this.args);
            try {
                log.info("[StreamX]execute Command {} ", command);
                final Process process = new ProcessBuilder()
                        .command(command.split("\\s+"))
                        .inheritIO()
                        .start();
                process.waitFor();
            } catch (final Exception e) {
                log.info(e.getMessage(), e);
            }
        });
        threadPoolExecutor.shutdown();
    }

}
