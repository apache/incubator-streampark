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

package org.apache.streampark.flink.client.bean;

import org.apache.flink.client.deployment.application.ApplicationConfiguration;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.CoreOptions;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class SessionClusterRestClientTest {

    @TempDir
    Path tempDir;

    @Test
    void submitsJarThroughRestApi() throws Exception {
        File jobJar = tempDir.resolve("job.jar").toFile();
        Files.write(jobJar.toPath(), new byte[]{1, 2, 3});

        AtomicReference<String> uploadBody = new AtomicReference<>();
        AtomicReference<String> runBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
            "/jars/upload",
            exchange -> {
                uploadBody.set(readBody(exchange));
                respond(exchange, "{\"filename\":\"/tmp/job.jar\",\"status\":\"success\"}");
            });
        server.createContext(
            "/jars/job.jar/run",
            exchange -> {
                runBody.set(readBody(exchange));
                respond(exchange, "{\"jobid\":\"job-123\"}");
            });
        server.start();

        try {
            Configuration configuration = new Configuration();
            configuration.set(ApplicationConfiguration.APPLICATION_MAIN_CLASS, "example.Main");
            configuration.set(
                ApplicationConfiguration.APPLICATION_ARGS, Arrays.asList("--name", "streampark"));
            configuration.set(CoreOptions.DEFAULT_PARALLELISM, 2);

            String jobId = SessionClusterRestClient.submit(
                "http://127.0.0.1:" + server.getAddress().getPort(), jobJar, configuration);

            assertThat(jobId).isEqualTo("job-123");
            assertThat(uploadBody.get()).contains("name=\"jarfile\"").contains("job.jar");
            assertThat(runBody.get())
                .contains("\"entryClass\":\"example.Main\"")
                .contains("\"parallelism\":\"2\"");
        } finally {
            server.stop(0);
        }
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static void respond(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
