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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandUtilsTest {

    @Test
    void executeSingleCommand() throws Exception {
        CommandUtils.CommandResult result = CommandUtils.execute("echo Hello");
        assertEquals(0, result.code);
        assertEquals("Hello\n", result.output);
    }

    @Test
    void executeMultipleCommands() throws Exception {
        String directory = System.getProperty("temp.dir");
        ArrayList<String> commands = new ArrayList<>();
        commands.add("echo 'Hello'");
        commands.add("echo 'World'");
        Consumer<String> outputConsumer = System.out::println;
        int exitCode = CommandUtils.execute(directory, commands, outputConsumer);
        assertEquals(0, exitCode);
    }
}
