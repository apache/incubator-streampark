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
package org.apache.streampark.console.base.util;

import freemarker.cache.StringTemplateLoader;
import freemarker.core.TemplateClassResolver;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.ui.freemarker.SpringTemplateLoader;

import java.io.IOException;
import java.io.StringWriter;

@Slf4j
public class FreemarkerUtils {

    private static final Configuration CONFIGURATION;

    static {
        SpringTemplateLoader templateLoader = new SpringTemplateLoader(new DefaultResourceLoader(),
            "classpath:alert-template");
        CONFIGURATION = new Configuration(Configuration.VERSION_2_3_28);
        CONFIGURATION.setNewBuiltinClassResolver(TemplateClassResolver.SAFER_RESOLVER);
        CONFIGURATION.setTemplateLoader(templateLoader);
        CONFIGURATION.setDefaultEncoding("UTF-8");
    }

    public static Template loadTemplateFile(String fileName) throws ExceptionInInitializerError {
        try {
            return CONFIGURATION.getTemplate(fileName);
        } catch (IOException e) {
            log.error("{} not found!", fileName);
            throw new ExceptionInInitializerError(fileName + " not found!");
        }
    }

    public static Template loadTemplateString(String template) throws Exception {
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_28);
        configuration.setNewBuiltinClassResolver(TemplateClassResolver.SAFER_RESOLVER);
        StringTemplateLoader stringTemplateLoader = new StringTemplateLoader();
        stringTemplateLoader.putTemplate("template", template);
        configuration.setTemplateLoader(stringTemplateLoader);
        return configuration.getTemplate("template");
    }

    public static String format(Template template, Object dataModel) throws TemplateException {
        try (StringWriter writer = new StringWriter()) {
            template.process(dataModel, writer);
            return writer.toString();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return null;
    }
}
