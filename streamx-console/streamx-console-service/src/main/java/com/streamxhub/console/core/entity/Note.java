/**
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.console.core.entity;

import com.wuwenze.poi.annotation.Excel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Properties;
import java.util.Scanner;

/**
 * @author benjobs
 */
@Excel("flink应用实体")
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
            StringBuffer codeBuffer = new StringBuffer();
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.startsWith("%flink.")) {
                    String[] dyProp = line.trim().split("\\=");
                    properties.setProperty(dyProp[0].substring(1), dyProp[1]);
                } else {
                    codeBuffer.append(line).append("\n");
                }
            }
            this.content = new Content(properties, codeBuffer.toString());
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

