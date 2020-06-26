package com.streamxhub.flink.monitor.core.entity;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.streamxhub.common.conf.ParameterCli;
import com.streamxhub.common.util.ThreadUtils;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.*;

@Data
@Slf4j
public class Deployment implements Serializable {

    private String name;
    private String basePath;
    private String confPath;
    private String binPath;
    private String libPath;
    private String logPath;
    private String tempPath;
    private String startup;
    private String stop;
    private String conf;
    private String args;
    private String jarFile;
    private String[] cliParam;

    private ThreadFactory namedThreadFactory = ThreadUtils.threadFactory("Flink-StartUp");

    public Deployment(Project project, Application application) {
        this.name = project.getName();
        this.basePath = project.getHome() + File.separator + this.name;
        this.confPath = this.basePath + "/conf";
        this.binPath = this.basePath + "/bin";
        this.libPath = this.basePath + "/lib";
        this.logPath = this.basePath + "/logs";
        this.tempPath = this.basePath + "/temp";
        this.startup = this.binPath + "/startup.sh";
        this.stop = this.binPath + "/stop.sh";
        this.conf = application.getConfigFile();
        this.jarFile = this.libPath + "/" + this.name + ".jar";
    }

    @SneakyThrows
    public void startUp() {
        ExecutorService threadPoolExecutor = Executors.newSingleThreadExecutor(namedThreadFactory);
        threadPoolExecutor.execute(() -> {
            String command = buildCommand();
            try {
                log.info("[StreamX]execute Command:>>{}<<", command);
                final Process process = new ProcessBuilder()
                        .command(command.split("\\s+"))
                        .inheritIO()
                        .start();
                process.waitFor();
            } catch (final Exception e) {
                log.info(e.getMessage(), e);
            }
        });
        ThreadUtils.shutdownExecutorService(threadPoolExecutor);
    }

    private String buildCommand() {
        String resource = readArgs("--resource");
        String dynamic = readArgs("--dynamic");
        String detached = readArgs("--detached");
        String name = readArgs("--name");
        String cmd = String.format("flink run %s %s --jar %s --flink.conf %s", resource, dynamic, jarFile, conf);
        StringBuffer cmdBuffer = new StringBuffer(cmd);
        if (detached.equals("Detached")) {
            String log = String.format("%s/%s-%s.log", logPath, name, System.currentTimeMillis());
            cmdBuffer.append(" >> ").append(log).append(" 2>&1 &");
        }
        return cmdBuffer.toString().trim();
    }

    private String readArgs(String action) {
        if (cliParam == null) {
            List<String> args = new ArrayList<String>();
            args.add(action);
            args.add(this.conf);
            args.addAll(Arrays.asList(this.args.split("\\s+")));
            String[] params = new String[args.size()];
            this.cliParam = args.toArray(params);
        } else {
            cliParam[0] = action;
        }
        return ParameterCli.read(cliParam);
    }

    private String flinkCommand(String... args) throws UnknownHostException, FileNotFoundException {
        String FLINK_HOME = System.getenv("FLINK_HOME");
        if (FLINK_HOME == null) {
            throw new ExceptionInInitializerError("[Streamx] System env FLINK_HOME can't found...");
        }
        String FLINK_LOG_DIR = FLINK_HOME.concat("/log");
        String FLINK_CONF_DIR = FLINK_HOME.concat("/conf");
        String FLINK_LIB_DIR = FLINK_HOME.concat("/lib");
        String USER = System.getProperty("user.name");
        String HOST = InetAddress.getLocalHost().getHostAddress();

        String log = String.format("%s/flink-%s-client-%s.log", FLINK_LOG_DIR, USER, HOST);
        String log_setting = String.format(" -Dlog.file=\"%s\" -Dlog4j.configuration=file:\"%s\"/log4j-cli.properties -Dlogback.configurationFile=file:\"%s\"/logback.xml",
                log,
                FLINK_CONF_DIR,
                FLINK_CONF_DIR
        );

        //build classpath...
        File file = new File(FLINK_LIB_DIR);
        StringBuffer cpBuffer = new StringBuffer(" -classpath ");
        for (File jar : file.listFiles()) {
            cpBuffer.append(jar.getAbsolutePath()).append(":");
        }

        String JAVA_RUN = getJavaRun();
        String HADOOP_CLASSPATH = System.getenv("HADOOP_CLASSPATH");
        String HBASE_CONF_DIR = System.getenv("HBASE_CONF_DIR");
        String HADOOP_CONF_DIR = getHadoopConfDir();
        String YARN_CONF_DIR = getYarnConfDir(FLINK_CONF_DIR);
        StringBuffer INTERNAL_HADOOP_CLASSPATHS = new StringBuffer();
        if (HADOOP_CLASSPATH != null) {
            INTERNAL_HADOOP_CLASSPATHS.append(HADOOP_CLASSPATH).append(":");
        }
        if (HADOOP_CONF_DIR != null) {
            INTERNAL_HADOOP_CLASSPATHS.append(HADOOP_CONF_DIR).append(":");
        }
        if (YARN_CONF_DIR != null) {
            INTERNAL_HADOOP_CLASSPATHS.append(YARN_CONF_DIR).append(":");
        }
        if (HBASE_CONF_DIR != null) {
            INTERNAL_HADOOP_CLASSPATHS.append(HBASE_CONF_DIR).append(":");
        }

        String classpath = cpBuffer.append(INTERNAL_HADOOP_CLASSPATHS).toString();

        StringBuffer command = new StringBuffer();

        String cliFrontend = " org.apache.flink.client.cli.CliFrontend ";

        command.append(JAVA_RUN)
                .append(log_setting)
                .append(classpath)
                .append(cliFrontend);
        return command.toString();
    }

    private String getJavaRun() {
        String JAVA_HOME = System.getenv("JAVA_HOME");
        return JAVA_HOME.concat("/bin/java");
    }

    private String getHadoopConfDir() {
        String HADOOP_HOME = System.getenv("HADOOP_HOME");
        String HADOOP_CONF_DIR = System.getenv("HADOOP_CONF_DIR");
        if (HADOOP_CONF_DIR == null) {
            if (HADOOP_HOME != null) {
                //HADOOP_HOME is set. Check if its a Hadoop 1.x or 2.x HADOOP_HOME path
                File h1Conf = new File(HADOOP_HOME, "conf");
                if (h1Conf.exists() && h1Conf.isDirectory()) {
                    // its a Hadoop 1.x
                    HADOOP_CONF_DIR = h1Conf.getAbsolutePath();
                }
                File h2Conf = new File(HADOOP_HOME.concat("/etc/hadoop"));
                if (h2Conf.exists() && h2Conf.isDirectory()) {
                    // Its Hadoop 2.2+
                    HADOOP_CONF_DIR = h2Conf.getAbsolutePath();
                }
            }
        }
        // try and set HADOOP_CONF_DIR to some common default if it's not set
        if (HADOOP_CONF_DIR == null) {
            File ectConf = new File("/etc/hadoop/conf");
            if (ectConf.exists() && ectConf.isDirectory()) {
                // Its Hadoop 2.2+
                System.out.println("Setting HADOOP_CONF_DIR=/etc/hadoop/conf because no HADOOP_CONF_DIR was set."
                );
                HADOOP_CONF_DIR = ectConf.getAbsolutePath();
            }
        }
        return HADOOP_CONF_DIR;
    }

    private String getYarnConfDir(String flinkConfDir) throws FileNotFoundException {
        String YAML_CONF = flinkConfDir.concat("/flink-conf.yaml");
        String KEY_ENV_YARN_CONF_DIR = "env.yarn.conf.dir";
        Map yaml = new Yaml().loadAs(new FileInputStream(new File(YAML_CONF)), HashMap.class);
        Object YARN_CONF_DIR = yaml.get(KEY_ENV_YARN_CONF_DIR);
        if (YARN_CONF_DIR != null) {
            return YARN_CONF_DIR.toString();
        }
        return null;
    }

}
