package com.streamxhub.spark.monitor.core.runner;

import com.streamxhub.spark.monitor.common.exception.RedisConnectException;
import com.streamxhub.spark.monitor.common.utils.IOUtils;
import com.streamxhub.spark.monitor.system.domain.User;
import com.streamxhub.spark.monitor.system.manager.UserManager;
import com.streamxhub.spark.monitor.system.service.CacheService;
import com.streamxhub.spark.monitor.system.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;


@Slf4j
@Component
public class InitializeRunner implements ApplicationRunner {

    @Autowired
    private UserService userService;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private UserManager userManager;

    @Autowired
    private ConfigurableApplicationContext context;

    @Override
    public void run(ApplicationArguments args) {
        String home = System.getProperty("streamx.home", null);
        if (home != null) {
            String executor = home.concat("/bin/executor.so");
            if (!IOUtils.fileExists(executor)) {
                log.error("[StreamX] can't found gcc. please compile executor.c first,please compile executor.c by yourself.");
                log.error(getLogo(false));
                context.close();
            }
        }

        try {
            log.info("Redis连接中 ······");
            cacheService.testConnect();
            log.info("缓存初始化 ······");
            log.info("缓存用户数据 ······");
            List<User> list = this.userService.list();
            for (User user : list) {
                userManager.loadUserRedisCache(user);
            }
        } catch (Exception e) {
            if (e instanceof RedisConnectException) {
                log.error("Redis连接异常，请检查Redis连接配置并确保Redis服务已启动");
            }
            log.error("缓存初始化失败，{}", e.getMessage());
            log.error(getLogo(false));
            context.close();
        }

        if (context.isActive()) {
            log.info(getLogo(true));
        }
    }

    private String getLogo(boolean success) {
        return "\n" +
                "     ,---.   ,--.                                  ,--.   ,--.  \n " +
                "    '   .-',-'  '-.,--.--. ,---.  ,--,--.,--,--,--. \\  `.'  /  \n" +
                "    `.  `-.'-.  .-'|  .--'| .-. :' ,-.  ||        |  .'    \\   \n" +
                "    .-'    | |  |  |  |   \\   --.\\ '-'  ||  |  |  | /  .'.  \\\n" +
                "    `-----'  `--'  `--'    `----' `--`--'`--`--`--''--'   '--'   \n\n" +
                String.format("    StreamX 启动%s，时间：" + LocalDateTime.now(), success ? "成功" : "失败");
    }
}
