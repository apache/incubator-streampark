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

package com.streamxhub.streamx.console.core.service.impl;

import com.streamxhub.streamx.common.util.ThreadUtils;
import com.streamxhub.streamx.console.core.entity.FlinkEnv;
import com.streamxhub.streamx.console.core.entity.Note;
import com.streamxhub.streamx.console.core.service.FlinkEnvService;
import com.streamxhub.streamx.console.core.service.NoteBookService;
import com.streamxhub.streamx.flink.repl.interpreter.FlinkInterpreter;
import com.streamxhub.streamx.flink.repl.interpreter.InterpreterOutput;
import com.streamxhub.streamx.flink.repl.interpreter.InterpreterResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author benjobs
 */
@Slf4j
@Service
public class NoteBookServiceImpl implements NoteBookService {

    @Autowired
    private FlinkEnvService flinkEnvService;

    private ExecutorService executorService = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors() * 2,
            200,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1024),
            ThreadUtils.threadFactory("notebook-submit-executor"),
            new ThreadPoolExecutor.AbortPolicy()
    );

    @Override
    public void submit(Note note) {
        Note.Content content = note.getContent();
        executorService.execute(() -> {
            FlinkInterpreter interpreter = new FlinkInterpreter(content.getProperties());
            try {
                FlinkEnv flinkEnv = flinkEnvService.getDefault();
                interpreter.open(flinkEnv.getFlinkHome());
                InterpreterOutput out = new InterpreterOutput(log::info);
                InterpreterResult result = interpreter.interpret(content.getCode(), out);
                log.info("repl submit code:" + result.code());
                if (result.code().equals(InterpreterResult.ERROR())) {
                    log.info("NoteBook submit error: {}", out.toString());
                } else if (result.code().equals(InterpreterResult.SUCCESS())) {
                    log.info("NoteBook submit success: {}", out.toString());
                }
            } catch (Throwable e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } finally {
                try {
                    interpreter.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void submit2(Note note) {
    }
}
