package com.streamxhub.spark.monitor.system.runner;

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
            log.info("");
            log.info("             ('-.     _  .-')  .-. .-')   ");
            log.info("             ( OO ).-.( \\( -O ) \\  ( OO )  ");
            log.info("  ,--.       / . --. / ,------. ,--. ,--.  ");
            log.info("  |  |.-')   | \\-.  \\  |   /`. '|  .'   /  ");
            log.info("  |  | OO ).-'-'  |  | |  /  | ||      /,  ");
            log.info("  |  |`-' | \\| |_.'  | |  |_.' ||     ' _) ");
            log.info(" (|  '---.'  |  .-.  | |  .  '.'|  .   \\   ");
            log.info("  |      |   |  | |  | |  |\\  \\ |  |\\   \\  ");
            log.info("  `------'   `--' `--' `--' '--'`--' '--'  ");
            log.info("  STREAMX 启动完毕，时间：" + LocalDateTime.now());
            log.info("");
        }
    }
}
