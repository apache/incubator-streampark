package com.streamxhub.flink.submit;


import com.streamxhub.common.conf.ConfigConst;
import com.streamxhub.common.util.HdfsUtils;
import com.streamxhub.common.util.PropertiesUtils;
import org.apache.flink.client.deployment.ClusterDeploymentException;
import org.apache.flink.client.deployment.ClusterSpecification;
import org.apache.flink.client.deployment.application.ApplicationConfiguration;
import org.apache.flink.client.program.ClusterClient;
import org.apache.flink.configuration.*;
import org.apache.flink.util.Preconditions;
import org.apache.flink.yarn.YarnClientYarnClusterInformationRetriever;
import org.apache.flink.yarn.YarnClusterDescriptor;
import org.apache.flink.yarn.YarnClusterInformationRetriever;
import org.apache.flink.yarn.configuration.YarnConfigOptions;
import org.apache.flink.yarn.configuration.YarnDeploymentTarget;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import scala.collection.immutable.Map;

import static com.streamxhub.common.conf.ConfigConst.*;

import java.io.File;
import java.util.*;

import static org.apache.flink.yarn.configuration.YarnConfigOptions.CLASSPATH_INCLUDE_USER_JAR;

public class AppSubmit {

    public static void main(String[] args) throws Exception {

        String flink_home = System.getenv("FLINK_HOME");

        //存放flink集群相关的jar包目录
        File flinkLibs = new File(flink_home.concat("/lib"));
        File plugins = new File(flink_home.concat("/plugins"));

        //用户jar
        String app_home = "/home/hst/workspace/streamx/streamx-flink/streamx-flink-test/target/streamx-flink-test-1.0.0";
        String app_conf = "hdfs:///streamx/workspace/streamx-flink-test-1.0.0/conf/application.yml";
        String appName = null;
        String appMain = null;
        if (app_conf.startsWith("hdfs:")) {
            String text = HdfsUtils.readFile(app_conf);
            Map<String, String> map = PropertiesUtils.fromYamlFile(text);
            appName = map.get(KEY_FLINK_APP_NAME()).get();
            appMain = map.get(KEY_FLINK_APP_MAIN()).get();
        } else {
            Map<String, String> map = PropertiesUtils.fromYamlFile(app_conf);
            appName = map.get(KEY_FLINK_APP_NAME()).get();
            appMain = map.get(KEY_FLINK_APP_MAIN()).get();
        }

        File flinkUserJar = new File(app_home.concat("/lib/streamx-flink-test-1.0.0.jar"));
        File flinkDistJar = new File(flink_home.concat("/lib/flink-dist_2.11-1.11.1.jar"));

        YarnClient yarnClient = YarnClient.createYarnClient();
        YarnConfiguration yarnConfiguration = new YarnConfiguration();
        yarnClient.init(yarnConfiguration);
        yarnClient.start();

        YarnClusterInformationRetriever clusterInformationRetriever = YarnClientYarnClusterInformationRetriever.create(yarnClient);
        //获取flink的配置
        Configuration flinkConfiguration = GlobalConfiguration.loadConfiguration(flink_home.concat("/conf"));


        flinkConfiguration.set(JobManagerOptions.TOTAL_PROCESS_MEMORY, MemorySize.ofMebiBytes(768))
                .set(TaskManagerOptions.TOTAL_PROCESS_MEMORY, MemorySize.parse("1g"))
                .set(AkkaOptions.ASK_TIMEOUT, "30 s")
                .set(DeploymentOptions.TARGET, YarnDeploymentTarget.APPLICATION.getName())
                .set(CLASSPATH_INCLUDE_USER_JAR, YarnConfigOptions.UserJarInclusion.FIRST.toString())
                //设置用户的jar
                .set(PipelineOptions.JARS, Collections.singletonList(flinkUserJar.toString()))
                //设置为application模式
                .set(DeploymentOptions.TARGET, YarnDeploymentTarget.APPLICATION.getName())
                //yarn application name
                .set(YarnConfigOptions.APPLICATION_NAME, appName)
                //yarn application Type
                .set(YarnConfigOptions.APPLICATION_TYPE, "StreamX Flink")
        ;

        //.set(YarnConfigOptions.PROVIDED_LIB_DIRS, Arrays.asList(flinkLibs.toString(), plugins.toString()))
        //.set(YarnConfigOptions.FLINK_DIST_JAR, flinkDistJar);

        YarnClusterDescriptor yarnClusterDescriptor = new YarnClusterDescriptor(
                flinkConfiguration,
                yarnConfiguration,
                yarnClient,
                clusterInformationRetriever,
                true);

        //设置flink_disk.jar
        yarnClusterDescriptor.setLocalJarPath(new Path(flinkDistJar.toURI()));
        //设置flink/lib
        yarnClusterDescriptor.addShipFiles(Arrays.asList(flinkLibs, plugins));

        final int masterMemory = yarnClusterDescriptor.getFlinkConfiguration().get(JobManagerOptions.TOTAL_PROCESS_MEMORY).getMebiBytes();
        ClusterSpecification clusterSpecification = new ClusterSpecification.ClusterSpecificationBuilder()
                .setMasterMemoryMB(masterMemory)
                .setTaskManagerMemoryMB(1024)
                .setSlotsPerTaskManager(1)
                .createClusterSpecification();


        final YarnDeploymentTarget deploymentTarget = YarnDeploymentTarget.fromConfig(flinkConfiguration);
        if (YarnDeploymentTarget.APPLICATION != deploymentTarget) {
            throw new ClusterDeploymentException(
                    "Couldn't deploy Yarn Application Cluster." +
                            " Expected deployment.target=" + YarnDeploymentTarget.APPLICATION.getName() +
                            " but actual one was \"" + deploymentTarget.getName() + "\"");
        }

        final List<String> pipelineJars = flinkConfiguration.getOptional(PipelineOptions.JARS).orElse(Collections.emptyList());
        Preconditions.checkArgument(pipelineJars.size() == 1, "Should only have one jar");

        //------------设置用户jar的参数和主类
        //设置启动主类
        flinkConfiguration.set(ApplicationConfiguration.APPLICATION_MAIN_CLASS, appMain);
        //设置启动参数
        flinkConfiguration.set(ApplicationConfiguration.APPLICATION_ARGS, Arrays.asList(KEY_FLINK_APP_CONF("--"), app_conf));

        ApplicationConfiguration applicationConfiguration = ApplicationConfiguration.fromConfiguration(flinkConfiguration);

        try (ClusterClient<ApplicationId> clusterClient = yarnClusterDescriptor
                .deployApplicationCluster(clusterSpecification, applicationConfiguration)
                .getClusterClient()) {

            ApplicationId applicationId = clusterClient.getClusterId();
            System.out.println("---------------------------------------");
            System.out.println();
            System.out.println("Flink Job Started: applicationId: " + applicationId);
            System.out.println();
            System.out.println("---------------------------------------");

        }

    }
}
