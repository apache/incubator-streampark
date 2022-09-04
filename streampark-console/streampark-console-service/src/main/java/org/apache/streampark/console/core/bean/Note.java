/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.streampark.console.core.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Properties;
import java.util.Scanner;

@Slf4j
@Data
public class Note {

    private String jobName;

    private String env;

    private String text;

    private Content content = null;

    public Content getContent() {
        if (this.content == null) {
            Scanner scanner = new Scanner(this.text);
            Properties properties = new Properties();
            StringBuilder codeBuilder = new StringBuilder();
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.startsWith("%flink.")) {
                    String[] dyProp = line.trim().split("=");
                    properties.setProperty(dyProp[0].substring(1), dyProp[1]);
                } else {
                    codeBuilder.append(line).append("\n");
                }
            }
            this.content = new Content(properties, codeBuilder.toString());
        }
        return this.content;
    }

    @Data
    @AllArgsConstructor
    public static class Content {
        private Properties properties;
        private String code;
    }
}
