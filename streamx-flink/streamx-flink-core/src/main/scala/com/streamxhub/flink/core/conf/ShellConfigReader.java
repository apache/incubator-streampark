package com.streamxhub.flink.core.conf;

import com.streamxhub.common.util.PropertiesUtils;
import org.apache.commons.lang3.StringUtils;
import scala.collection.JavaConversions;

import java.io.*;
import java.util.Map;

/**
 * @author benjobs
 */
public class ShellConfigReader implements Serializable {

    static String resourcePrefix = "flink.deployment.resource.";
    static String dynamicPrefix = "flink.deployment.dynamic.";

    public static void main(String[] args) {
        String action = args[0];
        String conf = args[1];
        scala.collection.immutable.Map<String, String> configArgs;
        if (conf.endsWith(".properties")) {
            configArgs = PropertiesUtils.fromPropertiesFile(conf);
        } else {
            configArgs = PropertiesUtils.fromYamlFile(conf);
        }
        Map<String, String> map = JavaConversions.mapAsJavaMap(configArgs);
        StringBuilder buffer = new StringBuilder();
        switch (action) {
            case "--resource":
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    if (StringUtils.isNoneBlank(entry.getValue()) && entry.getKey().startsWith(resourcePrefix)) {
                        buffer.append(" --")
                                .append(entry.getKey().replace(resourcePrefix, ""))
                                .append(" ")
                                .append(entry.getValue());
                    }
                }
                System.out.println(buffer.toString().trim());
                break;
            case "--dynamic":
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    if (StringUtils.isNoneBlank(entry.getValue()) && entry.getKey().startsWith(dynamicPrefix)) {
                        buffer.append(" -yD ")
                                .append(entry.getKey().replace(dynamicPrefix, ""))
                                .append("=")
                                .append(entry.getValue());
                    }
                }
                System.out.println(buffer.toString().trim());
                break;
            case "--name":
                String yarnName = map.getOrDefault("flink.deployment.resource.yarnname", null);
                if (StringUtils.isEmpty(yarnName)) {
                    System.out.println("");
                } else {
                    System.out.println(" --yarnname " + yarnName);
                }
                break;
            default:
                break;
        }

    }


}
