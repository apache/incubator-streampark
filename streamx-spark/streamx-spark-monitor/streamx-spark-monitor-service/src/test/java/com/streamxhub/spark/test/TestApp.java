package com.streamxhub.spark.test;

import org.apache.commons.lang3.RegExUtils;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class TestApp {


    @Test
    public void testReg() {
        String conf = "######################################################\n" +
                "#                                                    #\n" +
                "#               spark process startup.sh             #\n" +
                "#                   user config                      #\n" +
                "#                                                    #\n" +
                "######################################################\n" +
                "#必须设置,执行class的全包名称\n" +
                "spark.app.main=com.streamx.spark.test.HelloStreamXApp\n" +
                "#spark 任务名称配置,建议保持任务名称全局唯一\n" +
                "#这样可以在设计任务失败的时候根据名称做一些唯一处理\n" +
                "#不设置使用类全名.App\n" +
                "1spark.app.name=HelloStreamXApp\n" +
                "spark.app.conf.local.version=10\n" +
                "######################################################\n" +
                "#                                                    #\n" +
                "#                spark config                        #\n" +
                "#                                                    #\n" +
                "######################################################\n" +
                "#执行集群设置,不用设置,一般使用YARN\n" +
                "spark.master=yarn\n" +
                "#YARN部署模式\n" +
                "#default=cluster\n" +
                "spark.submit.deployMode=cluster\n" +
                "#spark-streaming每个批次间隔时间\n" +
                "#default=300\n" +
                "spark.batch.duration=5\n" +
                "#spark on yarn的任务提交队列\n" +
                "#default=defalut\n" +
                "spark.yarn.queue=default";

        if (Pattern.compile("\n(\\s+|)spark\\.app\\.main").matcher(conf).find()) {
            System.out.println("");
        }


    }

}
