package com.streamxhub.flink.core.conf;

import com.streamxhub.flink.core.util.PropertiesUtils;
import scala.collection.JavaConversions;

import java.io.*;
import java.util.Map;

public class ShellConfigReader implements Serializable {

    public static void main(String[] args) throws IOException {
        String action = args[0];
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (action.equals("--which")) {
            BufferedReader in = new BufferedReader(new InputStreamReader(loader.getResourceAsStream("./")));
            StringBuffer buffer = new StringBuffer();
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("application-") && (line.endsWith(".properties") || line.endsWith(".yml"))) {
                    buffer.append(line).append(" ");
                }
            }
            System.out.println(buffer.toString().trim());
        } else if (action.equals("--conf")) {
            String conf = args[1];
            scala.collection.immutable.Map<String, String> configArgs;
            if (conf.endsWith(".properties")) {
                configArgs = PropertiesUtils.fromPropertiesFile(loader.getResourceAsStream(".".concat(conf)));
            } else {
                configArgs = PropertiesUtils.fromYamlFile(loader.getResourceAsStream(".".concat(conf)));
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
