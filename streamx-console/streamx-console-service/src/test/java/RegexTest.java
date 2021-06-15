/*
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

import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RegexTest {

    @Test
    public void regex() {
        String jar = "streamx-flink-shims_flink-1.11-1.1.0-SNAPSHOT.jar";
        String shimsRegex = "streamx-flink-shims_flink-(1.11|1.12)-(.*).jar$";
        Pattern pattern = Pattern.compile(shimsRegex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(jar);
        if (matcher.matches()) {
            String version = matcher.group(1);
            System.out.println(version);
        }
    }

    @Test
    public void classLoader() throws MalformedURLException {
        List<URL> libCache = new ArrayList<>(0);
        ;
        List<URL> shimsCache = new ArrayList<>(0);
        ;
        String regex = "(^|.*)streamx-flink-shims_flink-(1.12|1.13)-(.*).jar$";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        String lib = "~/workspace/streamx/streamx-console-service-1.1.0-SNAPSHOT/lib";

        File[] libJars = new File(lib).listFiles(pathname -> !pathname.getName().matches(regex));
        assert libJars != null;
        for (File jar : libJars) {
            libCache.add(jar.toURI().toURL());
        }

        File[] shimsJars = new File(lib).listFiles(pathname -> pathname.getName().matches(regex));
        assert shimsJars != null;
        for (File jar : shimsJars) {
            shimsCache.add(jar.toURI().toURL());
        }

        String version = "1.13";
        List<URL> shimsUrls = shimsCache.stream().filter(url -> {
            Matcher matcher = pattern.matcher(url.getFile());
            if (matcher.matches()) {
                System.out.println(matcher.group(2));
                return version.equals(matcher.group(2));
            }
            return false;
        }).collect(Collectors.toList());
        shimsUrls.addAll(libCache);
        URLClassLoader urlClassLoader = new URLClassLoader(shimsUrls.toArray(new URL[0]));
        System.out.println(urlClassLoader);
    }

}
