package com.streamxhub.flink.core.conf;

import com.streamxhub.flink.core.util.PropertiesUtils;
import org.apache.commons.lang3.StringUtils;
import scala.collection.JavaConversions;

import java.io.*;
import java.util.Map;

public class ShellConfigReader implements Serializable {

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
        if(conf.equals("--deploy")) {
            StringBuffer buffer = new StringBuffer();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                if (StringUtils.isNoneBlank(entry.getValue()) && entry.getKey().startsWith("flink.deploy")) {
                    buffer.append(" --")
                            .append(entry.getKey().replace("flink.deploy.", ""))
                            .append(" ")
                            .append(entry.getValue());
                }
            }
            System.out.println(buffer.toString().trim());
        } else if (action.equals("--conf")) {
            StringBuffer buffer = new StringBuffer();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                if (StringUtils.isNoneBlank(entry.getValue()) && !entry.getKey().startsWith("flink.deploy")) {
                    buffer.append(" -D").append(entry.getKey()).append("=").append(entry.getValue());
                }
            }
            System.out.println(buffer.toString().trim());
        }
    }

}
