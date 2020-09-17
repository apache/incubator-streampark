package com.streamxhub.console.system.runner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Order
@Slf4j
@Component
public class StartedUpRunner implements ApplicationRunner {

    @Autowired
    private ConfigurableApplicationContext context;

    @Override
    public void run(ApplicationArguments args) {
        if (context.isActive()) {
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
            log.info("            StreamX 启动完毕，时间：" + LocalDateTime.now());
        }
    }
}
