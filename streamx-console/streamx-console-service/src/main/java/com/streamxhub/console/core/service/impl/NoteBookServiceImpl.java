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
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.runtime.akka.AkkaUtils;
import org.apache.zeppelin.display.AngularObjectRegistry;
import org.apache.zeppelin.flink.FlinkInterpreter;
import org.apache.zeppelin.interpreter.*;
import org.apache.zeppelin.interpreter.remote.RemoteInterpreterEventClient;
import org.springframework.stereotype.Service;

import static org.mockito.Mockito.mock;
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
        Executors.newSingleThreadExecutor().submit(() -> {
            Properties properties = new Properties();
            properties.setProperty("zeppelin.flink.printREPLOutput", "true");
            properties.setProperty("zeppelin.flink.scala.color", "true");
            properties.setProperty("flink.yarn.queue", "root.users.hst");
            properties.setProperty("flink.execution.mode", "yarn");
            properties.setProperty("akka.ask.timeout","10s");

            FlinkInterpreter   interpreter = new FlinkInterpreter(properties);
            InterpreterGroup interpreterGroup = new InterpreterGroup();
            interpreter.setInterpreterGroup(interpreterGroup);
            try {
                interpreter.open();
                AngularObjectRegistry angularObjectRegistry = new AngularObjectRegistry("flink", null);
                InterpreterContext context = InterpreterContext.builder()
                        .setParagraphId("paragraphId")
                        .setAngularObjectRegistry(angularObjectRegistry)
                        .setIntpEventClient(mock(RemoteInterpreterEventClient.class))
                        .setInterpreterOut(new InterpreterOutput(null))
                        .build();
                InterpreterContext.set(context);
                InterpreterResult result = interpreter.interpret(note.getSourceCode(), context);
                System.out.println(context.out.toString());
                assert InterpreterResult.Code.SUCCESS.equals(result.code());
            } catch (Throwable e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } finally {
                if (interpreter != null) {
                    try {
                        interpreter.close();
                    } catch (InterpreterException e) {
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
