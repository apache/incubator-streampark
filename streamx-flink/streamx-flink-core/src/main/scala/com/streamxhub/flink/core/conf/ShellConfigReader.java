package com.streamxhub.flink.core.conf;

import com.streamxhub.flink.core.util.PropertiesUtils;
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
            StringBuffer stringBuffer = new StringBuffer();
            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();
                String[] entryInfo = jarEntry.getName().split("\\.");
                if (entryInfo.length == 2) {
                    String name = entryInfo[0];
                    String type = entryInfo[1];
                    if(name.startsWith("application-")) {
                        if(type.equals("properties") || type.equals("yml")) {
                            stringBuffer.append(jarEntry.getName()).append(" ");
                        }
                    }
                }
            }
            System.out.println(stringBuffer.toString().trim());
        } else if (action.equals("--read")) {
            JarFile jarFile = new JarFile(jarPath);
            String conf = args[2];
            JarEntry jarEntry = jarFile.getJarEntry(conf);
            scala.collection.immutable.Map<String, String> configArgs;
            if (conf.endsWith(".properties")) {
                configArgs = PropertiesUtils.fromPropertiesFile(jarFile.getInputStream(jarEntry));
            } else {
                configArgs = PropertiesUtils.fromYamlFile(jarFile.getInputStream(jarEntry));
            }
            Map<String, String> map = JavaConversions.mapAsJavaMap(configArgs);
            StringBuffer buffer = new StringBuffer();
            for (Map.Entry<String, String> entry : map.entrySet()) {
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
