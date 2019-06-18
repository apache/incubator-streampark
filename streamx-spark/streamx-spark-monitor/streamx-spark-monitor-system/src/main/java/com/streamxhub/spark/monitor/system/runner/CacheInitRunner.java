package com.streamxhub.spark.monitor.system.runner;

import com.streamxhub.spark.monitor.common.exception.RedisConnectException;
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

/**
 * 缓存初始化
 */
@Slf4j
@Component
public class CacheInitRunner implements ApplicationRunner {

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
            log.error("");
            log.error("              ('-.     _  .-')  .-. .-')   ");
            log.error("             ( OO ).-.( \\( -O ) \\  ( OO )  ");
            log.error("  ,--.       / . --. / ,------. ,--. ,--.  ");
            log.error("  |  |.-')   | \\-.  \\  |   /`. '|  .'   /  ");
            log.error("  |  | OO ).-'-'  |  | |  /  | ||      /,  ");
            log.error("  |  |`-' | \\| |_.'  | |  |_.' ||     ' _) ");
            log.error(" (|  '---.'  |  .-.  | |  .  '.'|  .   \\   ");
            log.error("  |      |   |  | |  | |  |\\  \\ |  |\\   \\  ");
            log.error("  `------'   `--' `--' `--' '--'`--' '--'  ");
            log.error("");
            log.error("  STREAMX 启动完毕，时间：" + LocalDateTime.now());
            log.error("");
            context.close();
        }
    }
}
