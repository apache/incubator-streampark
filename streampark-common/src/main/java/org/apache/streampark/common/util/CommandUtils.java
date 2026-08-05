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

package org.apache.streampark.common.util;

import org.apache.streampark.shaded.org.slf4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;

public final class CommandUtils {

    private static final Logger LOG = StreamParkLoggerFactory.loggerFactory().getLogger(CommandUtils.class.getName());
    private CommandUtils() {
    }

    public static class CommandResult {

        public final int code;
        public final String output;
        public CommandResult(int code, String output) {
            this.code = code;
            this.output = output;
        }
    }

    public static CommandResult execute(String command) {
        try {
            StringBuffer buffer = new StringBuffer();
            Process process = Runtime.getRuntime().exec(command);
            InputStreamReader reader = new InputStreamReader(process.getInputStream());
            Scanner scanner = new Scanner(reader);
            while (scanner.hasNextLine())
                buffer.append(scanner.nextLine()).append("\n");
            int code = waitFor(process);
            reader.close();
            scanner.close();
            return new CommandResult(code, buffer.toString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static int execute(String directory, Iterable<String> commands, Consumer<String> consumer) {
        try {
            if (commands == null || !commands.iterator().hasNext()) {
                throw new IllegalArgumentException("[StreamPark] CommandUtils.execute: commands must not be null.");
            }
            StringBuilder debug = new StringBuilder();
            for (String c : commands)
                debug.append(c).append("\n");
            LOG.debug("[StreamPark] Command execute:\n{}", debug);
            List<String> interpreters = Utils.isWindows() ? Arrays.asList("cmd", "/k") : Arrays.asList("/bin/bash");
            ProcessBuilder builder = new ProcessBuilder(interpreters).redirectErrorStream(true);
            if (directory != null)
                builder.directory(new File(directory));
            Process process = builder.start();
            PrintWriter out =
                new PrintWriter(new BufferedWriter(new OutputStreamWriter(process.getOutputStream())), true);
            String last = null;
            for (String cmd : commands) {
                out.println(cmd);
                last = cmd;
            }
            if (last == null || !last.equalsIgnoreCase("exit"))
                out.println("exit");
            out.close();
            Scanner scanner = new Scanner(process.getInputStream());
            while (scanner.hasNextLine())
                consumer.accept(scanner.nextLine());
            scanner.close();
            return waitFor(process);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static int waitFor(Process process) throws InterruptedException, java.io.IOException {
        int code = process.waitFor();
        process.getErrorStream().close();
        process.getInputStream().close();
        process.getOutputStream().close();
        process.destroy();
        return code;
    }
}
