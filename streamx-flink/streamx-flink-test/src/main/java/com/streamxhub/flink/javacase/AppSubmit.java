package com.streamxhub.flink.javacase;


import org.apache.flink.client.deployment.ClusterDeploymentException;
import org.apache.flink.client.deployment.ClusterSpecification;
import org.apache.flink.client.deployment.application.ApplicationConfiguration;
import org.apache.flink.client.program.ClusterClient;
import org.apache.flink.client.program.ClusterClientProvider;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.DeploymentOptions;
import org.apache.flink.configuration.GlobalConfiguration;
import org.apache.flink.configuration.PipelineOptions;
import org.apache.flink.yarn.YarnClientYarnClusterInformationRetriever;
import org.apache.flink.yarn.YarnClusterDescriptor;
import org.apache.flink.yarn.YarnClusterInformationRetriever;
import org.apache.flink.yarn.configuration.YarnConfigOptions;
import org.apache.flink.yarn.configuration.YarnDeploymentTarget;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.conf.YarnConfiguration;

import java.util.Arrays;
import java.util.Collections;

public class AppSubmit {

    public static void main(String[] args) {
        //flink的本地配置目录，为了得到flink的配置
        String configurationDirectory = System.getenv("FLINK_HOME").concat("/conf");
        //存放flink集群相关的jar包目录
        Path flinkLibs = new Path("hdfs:///streamx/flink/flink-1.9.2/lib");
        Path plugins = new Path("hdfs:///streamx/flink/flink-1.9.2/plugins");
        //用户jar
        String userJarPath = "hdfs:///streamx/workspace/streamx-flink-test-1.0.0/lib/streamx-flink-test-1.0.0.jar";
        String flinkDistJar = "hdfs:///streamx/flink/flink-1.9.2/lib/flink-dist_2.11-1.11.1.jar";

        YarnClient yarnClient = YarnClient.createYarnClient();
        YarnConfiguration yarnConfiguration = new YarnConfiguration();
        yarnClient.init(yarnConfiguration);
        yarnClient.start();

        YarnClusterInformationRetriever clusterInformationRetriever = YarnClientYarnClusterInformationRetriever.create(yarnClient);
        //获取flink的配置
        Configuration flinkConfiguration = GlobalConfiguration.loadConfiguration(configurationDirectory);

        flinkConfiguration
                .set(PipelineOptions.JARS, Collections.singletonList(userJarPath))
                .set(YarnConfigOptions.PROVIDED_LIB_DIRS, Arrays.asList(flinkLibs.toString(), plugins.toString()))
                .set(YarnConfigOptions.FLINK_DIST_JAR, flinkDistJar)
                .set(DeploymentOptions.TARGET, YarnDeploymentTarget.APPLICATION.getName()) //设置为application模式
                .set(YarnConfigOptions.APPLICATION_NAME, "jobName");//yarn application name


        ClusterSpecification clusterSpecification = new ClusterSpecification.ClusterSpecificationBuilder().createClusterSpecification();

        //设置用户jar的参数和主类
        String[] param = new String[2];
        param[0] = "--flink.conf";
        param[1] = "hdfs:///streamx/workspace/streamx-flink-test-1.0.0/conf/application.yml";

        ApplicationConfiguration appConfig = new ApplicationConfiguration(param, "com.streamxhub.flink.test.FlinkSinkApp");

        YarnClusterDescriptor yarnClusterDescriptor = new YarnClusterDescriptor(
                flinkConfiguration,
                yarnConfiguration,
                yarnClient,
                clusterInformationRetriever,
                true);

        ClusterClientProvider<ApplicationId> clusterClientProvider = null;
        try {
            clusterClientProvider = yarnClusterDescriptor.deployApplicationCluster(clusterSpecification, appConfig);  assert clusterClientProvider != null;
            ClusterClient<ApplicationId> clusterClient = clusterClientProvider.getClusterClient();
            ApplicationId applicationId = clusterClient.getClusterId();
            System.out.println(applicationId);
        } catch (ClusterDeploymentException e) {
            e.printStackTrace();
        }

    }

}
