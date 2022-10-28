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

package java.util.regex;

import org.apache.streampark.common.util.CommandUtils;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class RegexTest {

    @Test
    void regex() {
        String jar = "streampark-flink-shims_flink-1.11-1.1.0-SNAPSHOT.jar";
        String shimsRegex = "streampark-flink-shims_flink-(1.11|1.12)-(.*).jar$";
        Pattern pattern = Pattern.compile(shimsRegex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(jar);
        if (matcher.matches()) {
            String version = matcher.group(1);
            System.out.println(version);
        }
    }

    @Test
    void classLoader() throws MalformedURLException {
        List<URL> libCache = new ArrayList<>(0);
        List<URL> shimsCache = new ArrayList<>(0);
        String regex = "(^|.*)streampark-flink-shims_flink-(1.12|1.13|1.14|1.15|1.16)-(.*).jar$";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        String lib = "~/workspace/streampark/streampark-console-service-1.1.0-SNAPSHOT/lib";

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

    @Test
    void flinkVersion() {
        final Pattern flinkVersionPattern = Pattern.compile("^Version: (.*), Commit ID: (.*)$");
        String flinkHome = System.getenv("FLINK_HOME");
        String libPath = flinkHome.concat("/lib");
        File[] distJar = new File(libPath).listFiles(x -> x.getName().matches("flink-dist.*\\.jar"));
        if (distJar == null || distJar.length == 0) {
            throw new IllegalArgumentException("[StreamPark] can no found flink-dist jar in " + libPath);
        }
        if (distJar.length > 1) {
            throw new IllegalArgumentException("[StreamPark] found multiple flink-dist jar in " + libPath);
        }
        List<String> cmd = Collections.singletonList(
            String.format(
                "java -classpath %s org.apache.flink.client.cli.CliFrontend --version",
                distJar[0].getAbsolutePath()
            )
        );

        CommandUtils.execute(flinkHome, cmd, versionInfo -> {
            Matcher matcher = flinkVersionPattern.matcher(versionInfo);
            if (matcher.find()) {
                String flinkVersion = matcher.group(1);
                System.out.println(flinkVersion);
            }
        });
    }

    @Test
    void jobName() {
        final Pattern jobNamePattern = Pattern.compile("^[.\\x{4e00}-\\x{9fa5}A-Za-z0-9_\\-\\s]+$");
        String jobName = "flink-sql demo";
        if (jobNamePattern.matcher(jobName).matches()) {
            final Pattern namePattern = Pattern.compile("^[^\\s]+(\\s[^\\s]+)*$");
            if (namePattern.matcher(jobName).matches()) {
                System.out.println("passed");
            }
        }
    }

}
