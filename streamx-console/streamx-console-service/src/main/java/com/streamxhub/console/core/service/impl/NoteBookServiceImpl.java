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
package com.streamxhub.console.core.service.impl;

import com.streamxhub.console.core.entity.Note;
import com.streamxhub.console.core.service.NoteBookService;
import com.streamxhub.repl.flink.interpreter.FlinkInterpreter;
import com.streamxhub.repl.flink.interpreter.FlushListener;
import com.streamxhub.repl.flink.interpreter.InterpreterOutput;
import com.streamxhub.repl.flink.interpreter.InterpreterResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Properties;
import java.util.concurrent.Executors;

/**
 * @author benjobs
 */
@Slf4j
@Service
public class NoteBookServiceImpl implements NoteBookService {

    @Override
    public void submit(Note note) {
        Properties properties = new Properties();
        properties.setProperty("repl.out", "true");
        properties.setProperty("flink.yarn.queue", "root.users.hst");
        properties.setProperty("flink.execution.mode", "yarn");
        Executors.newSingleThreadExecutor().submit(() -> {
            FlinkInterpreter interpreter = new FlinkInterpreter(properties);
            try {
                InterpreterOutput out = new InterpreterOutput(line -> {
                    //TODO, show line to html.
                    //
                });
                InterpreterResult result = interpreter.interpret(note.getSourceCode(), out);
                System.out.println("[StreamX] repl submit code:" + result.code());
                if (result.code().equals(InterpreterResult.ERROR())) {
                    log.info("[StreamX] NoteBook submit error: {}", out.toString());
                } else if (result.code().equals(InterpreterResult.SUCCESS())) {
                    log.info("[StreamX] NoteBook submit success: {}", out.toString());
                }
            } catch (Throwable e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } finally {
                if (interpreter != null) {
                    try {
                        interpreter.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public void submit2(Note note) {

    }
}
