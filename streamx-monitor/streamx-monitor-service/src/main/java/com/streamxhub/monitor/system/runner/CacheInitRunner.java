package com.streamxhub.monitor.system.runner;

import com.streamxhub.monitor.base.exception.RedisConnectException;
import com.streamxhub.monitor.system.entity.User;
import com.streamxhub.monitor.system.manager.UserManager;
import com.streamxhub.monitor.system.service.CacheService;
import com.streamxhub.monitor.system.service.UserService;
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
                log.info("Redis连接异常，请检查Redis连接配置并确保Redis服务已启动");
            }
            log.info("缓存初始化失败，{}", e.getMessage());
            log.info("                                                            ");
            log.info("                          ▒▓██▓██▒                          ");
            log.info("                      ▓████▒▒█▓▒▓███▓▒                      ");
            log.info("                   ▓███▓░░        ▒▒▒▓██▒  ▒                ");
            log.info("                 ░██▒   ▒▒▓▓█▓▓▒░      ▒████                ");
            log.info("                 ██▒         ░▒▓███▒    ▒█▒█▒               ");
            log.info("                   ░▓█            ███   ▓░▒██               ");
            log.info("                     ▓█       ▒▒▒▒▒▓██▓░▒░▓▓█               ");
            log.info("                   █░ █   ▒▒░       ███▓▓█ ▒█▒▒▒            ");
            log.info("                   ████░   ▒▓█▓      ██▒▒▒ ▓███▒            ");
            log.info("                ░▒█▓▓██       ▓█▒    ▓█▒▓██▓ ░█░            ");
            log.info("          ▓░▒▓████▒ ██         ▒█    █▓░▒█▒░▒█▒             ");
            log.info("         ███▓░██▓  ▓█           █   █▓ ▒▓█▓▓█▒              ");
            log.info("       ░██▓  ░█░            █  █▒ ▒█████▓▒ ██▓░▒            ");
            log.info("      ███░ ░ █░          ▓ ░█ █████▒░░    ░█░▓  ▓░          ");
            log.info("     ██▓█ ▒▒▓▒          ▓███████▓░       ▒█▒ ▒▓ ▓██▓        ");
            log.info("  ▒██▓ ▓█ █▓█       ░▒█████▓▓▒░         ██▒▒  █ ▒  ▓█▒      ");
            log.info("  ▓█▓  ▓█ ██▓ ░▓▓▓▓▓▓▓▒              ▒██▓           ░█▒     ");
            log.info("  ▓█    █ ▓███▓▒░              ░▓▓▓███▓          ░▒░ ▓█     ");
            log.info("  ██▓    ██▒    ░▒▓▓███▓▓▓▓▓██████▓▒            ▓███  █     ");
            log.info(" ▓███▒ ███   ░▓▓▒░░   ░▓████▓░                  ░▒▓▒  █▓    ");
            log.info(" █▓▒▒▓▓██  ░▒▒░░░▒▒▒▒▓██▓░                            █▓    ");
            log.info(" ██ ▓░▒█   ▓▓▓▓▒░░  ▒█▓       ▒▓▓██▓    ▓▒          ▒▒▓     ");
            log.info(" ▓█▓ ▓▒█  █▓░  ░▒▓▓██▒            ░▓█▒   ▒▒▒░▒▒▓█████▒      ");
            log.info("  ██░ ▓█▒█▒  ▒▓▓▒  ▓█                █░      ░░░░   ░█▒     ");
            log.info("  ▓█   ▒█▓   ░     █░                ▒█              █▓     ");
            log.info("   █▓   ██         █░                 ▓▓        ▒█▓▓▓▒█░    ");
            log.info("    █▓ ░▓██░       ▓▒                  ▓█▓▒░░░▒▓█░    ▒█    ");
            log.info("     ██   ▓█▓░      ▒                    ░▒█▒██▒      ▓▓    ");
            log.info("      ▓█▒   ▒█▓▒░                         ▒▒ █▒█▓▒▒░░▒██    ");
            log.info("       ░██▒    ▒▓▓▒                     ▓██▓▒█▒ ░▓▓▓▓▒█▓    ");
            log.info("         ░▓██▒                          ▓░  ▒█▓█  ░░▒▒▒     ");
            log.info("             ▒▓▓▓▓▓▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒░░▓▓  ▓░▒█░       ");
            log.info("                                                            ");
            log.info("             +----------------------+                       ");
            log.info("             +  十步杀一人，千里不留行  +                       ");
            log.info("             +  事了拂衣去，深藏身与名  +                       ");
            log.info("             +----------------------+                       ");
            log.info("                                                            ");
            log.info("            [StreamX] let's flink|spark easy ô‿ô!           ");
            log.info("             StreamX 启动完毕，时间：" + LocalDateTime.now());
            log.info("");
            context.close();
        }
    }
}
