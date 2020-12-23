package com.streamxhub.console.system.runner;

import com.streamxhub.common.conf.ConfigConst;
import com.streamxhub.common.util.HdfsUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;


@Order
@Slf4j
@Component
public class EnvInitializeRunner implements ApplicationRunner {


    @Autowired
    private ApplicationContext context;

    private String PROD_ENV_NAME = "prod";

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String profiles = context.getEnvironment().getActiveProfiles()[0];
        if (profiles.equals(PROD_ENV_NAME)) {
            /**
             * init config....
             */
            String flinkLocalHome = System.getenv("FLINK_HOME");
            assert flinkLocalHome != null;
            log.info("[StreamX] flinkHome: {}", flinkLocalHome);

            String flinkName = new File(flinkLocalHome).getName();
            String flinkHdfsHome = ConfigConst.APP_FLINK().concat("/").concat(flinkName);

            if (!HdfsUtils.exists(flinkHdfsHome)) {
                log.info("[StreamX] {} is not exists,upload beginning....", flinkHdfsHome);
                HdfsUtils.upload(flinkLocalHome, flinkHdfsHome);
            }
            String flinkHdfsHomeWithNameService = HdfsUtils.getDefaultFS().concat(flinkHdfsHome);
            String flinkHdfsPlugins = flinkHdfsHomeWithNameService.concat("/plugins");
            //加载streamx下的plugins到$FLINK_HOME/plugins下
            loadPlugins(flinkHdfsPlugins);
        }else {
            log.warn("[StreamX]The local test environment is only used in the development phase to provide services to the console web, and many functions will not be available...");
        }
    }

    /**
     * 加载streamx的plugins到flink下的plugins下.
     *
     * @param pluginPath
     */
    private void loadPlugins(String pluginPath) {
        log.info("[StreamX] loadPlugins starting...");
        String appHome = System.getProperty("app.home");
        File streamXPlugins = new File(appHome, "plugins");
        Arrays.stream(streamXPlugins.listFiles()).forEach(x -> {
            String plugin = pluginPath.concat("/").concat(x.getName());
            if (!HdfsUtils.exists(plugin)) {
                log.info("[StreamX] load plugin:{} to {}", x.getName(), pluginPath);
                try {
                    HdfsUtils.upload(x.getAbsolutePath(), pluginPath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }


}
