/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.console.base.util;

import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.Enumeration;

/**
 * @author WeiJinglun
 * @date 2022.01.17
 */
@Slf4j
public class FreemarkerUtils {
    public static Template loadTemplateFile(String fileName) throws Exception {
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_28);
        Enumeration<URL> urls = ClassLoader.getSystemResources(fileName);
        if (urls != null) {
            if (!urls.hasMoreElements()) {
                urls = Thread.currentThread().getContextClassLoader().getResources(fileName);
            }

            if (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                if (url.getPath().contains(".jar")) {
                    configuration.setClassLoaderForTemplateLoading(Thread.currentThread().getContextClassLoader(), "");
                } else {
                    File file = new File(url.getPath());
                    configuration.setDirectoryForTemplateLoading(file.getParentFile());
                }
                configuration.setDefaultEncoding("UTF-8");
                return configuration.getTemplate(fileName);
            }
        }
        log.error("{} not found!", fileName);
        throw new ExceptionInInitializerError(fileName + " not found!");
    }

    public static Template loadTemplateString(String template) throws Exception {
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_28);
        StringTemplateLoader stringTemplateLoader = new StringTemplateLoader();
        stringTemplateLoader.putTemplate("template", template);
        configuration.setTemplateLoader(stringTemplateLoader);
        return configuration.getTemplate("template");
    }

    public static String format(Template template, Object dataModel) throws TemplateException {
        String result = null;
        try (StringWriter writer = new StringWriter()) {
            template.process(dataModel, writer);
            result = writer.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
}
