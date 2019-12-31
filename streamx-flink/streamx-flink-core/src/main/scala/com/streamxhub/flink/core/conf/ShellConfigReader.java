package com.streamxhub.flink.core.conf;

import com.streamxhub.flink.core.util.PropertiesUtils;
import scala.collection.JavaConversions;

import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;

public class ShellConfigReader implements Serializable {

    public static void main(String[] args) {
        String action = args[0];
        if(action.equals("--which")) {
            URL url = Thread.currentThread().getContextClassLoader().getResource("./");
            File[] files = new File(url.getFile()).listFiles((dir, name) -> name.matches(".*\\.properties$|.*\\.yml$"));
            String prop = Arrays.stream(files).map(x-> x.getName()+" ").toString();
            System.out.println(prop);
        }else if(action.equals("--conf")) {
            String conf = args[1];
            URL url= Thread.currentThread().getContextClassLoader().getResource(".".concat(conf));
            scala.collection.immutable.Map<String,String> configArgs = null;
            if(url.getPath().endsWith(".properties")) {
                configArgs = PropertiesUtils.fromPropertiesFile(url.getPath());
            }else if(url.getPath().endsWith(".yml")) {
                configArgs = PropertiesUtils.fromYamlFile(url.getPath());
            }else {
                throw new IllegalArgumentException("[StreamX] Usage:properties-file format error,muse be properties or yml");
            }
            Map<String,String> map = JavaConversions.mapAsJavaMap(configArgs);
            StringBuffer buffer = new StringBuffer();
            for (Map.Entry<String,String> entry:map.entrySet()) {
                if (entry.getKey().startsWith("flink.deploy")) {
                    buffer.append(" --".contains(entry.getKey()))
                            .append(" ")
                            .append(entry.getValue());
                }
            }
            System.out.println(buffer.toString());
        }
    }

}
