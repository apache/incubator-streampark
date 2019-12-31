package com.streamxhub.flink.core.conf;

import com.streamxhub.flink.core.util.PropertiesUtils;
import org.apache.commons.lang3.StringUtils;
import scala.collection.JavaConversions;

import java.io.*;
import java.util.Enumeration;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ShellConfigReader implements Serializable {

    public static void main(String[] args) throws IOException {
        String action = args[0];
        String jarPath = args[1];

        if (action.equals("--conf")) {
            JarFile jarFile = new JarFile(jarPath);
            Enumeration<JarEntry> entries = jarFile.entries();
            StringBuffer configBuffer = new StringBuffer();
            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();
                String[] entryInfo = jarEntry.getName().split("\\.");
                if (entryInfo.length == 2) {
                    String name = entryInfo[0];
                    String type = entryInfo[1];
                    //配置文件必须以application为前缀。。。。
                    if (name.startsWith("application-")) {
                        if (type.equals("properties") || type.equals("yml")) {
                            configBuffer.append("\"").append(jarEntry.getName()).append("\"").append(" ");
                        }
                    }
                }
            }
            System.out.println(configBuffer.toString().trim());
        } else if (action.equals("--read")) {
            JarFile jarFile = new JarFile(jarPath);
            String conf = args[2];
            JarEntry jarEntry = jarFile.getJarEntry(conf.replace("\"", ""));
            scala.collection.immutable.Map<String, String> configArgs;
            if (conf.endsWith(".properties")) {
                configArgs = PropertiesUtils.fromPropertiesFile(jarFile.getInputStream(jarEntry));
            } else {
                configArgs = PropertiesUtils.fromYamlFile(jarFile.getInputStream(jarEntry));
            }
            Map<String, String> map = JavaConversions.mapAsJavaMap(configArgs);

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
        }
    }

}
