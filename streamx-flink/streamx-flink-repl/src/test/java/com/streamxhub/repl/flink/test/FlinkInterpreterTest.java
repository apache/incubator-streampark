package com.streamxhub.repl.flink.test;
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


import com.streamxhub.repl.flink.interpreter.FlinkInterpreter;
import com.streamxhub.repl.flink.interpreter.InterpreterOutput;
import com.streamxhub.repl.flink.interpreter.InterpreterResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;


public class FlinkInterpreterTest {

    private FlinkInterpreter interpreter;

    @Before
    public void setUp() {
        Properties p = new Properties();
        p.setProperty("repl.out", "true");
        p.setProperty("scala.color", "false");
        p.setProperty("flink.execution.mode", "yarn");
        p.setProperty("local.number-taskmanager", "4");
        interpreter = new FlinkInterpreter(p);
    }

    @After
    public void tearDown() throws Exception {
        if (interpreter != null) {
            interpreter.close();
        }
    }

    @Test
    public void testWordCount() throws Exception {
        try {
            InterpreterOutput out = new InterpreterOutput(line -> {

            });

            interpreter.open();

            InterpreterResult result = interpreter.interpret(
                    "class WoldCount(val wold: String,val count: Int)",out
            );

            System.out.println(result.code().equals(InterpreterResult.SUCCESS()));

            result = interpreter.interpret(
                    "val data = env.fromElements(\"hello world\", \"hello flink\", \"hello hadoop\")\n" +
                            "\n" +
                            "data.flatMap(line => line.split(\"\\\\s\"))\n" +
                            "  .map(x=>new WoldCount(x,1))\n" +
                            "  .keyBy(_.wold)\n" +
                            "  .sum(\"count\")\n" +
                            "  .print()\n" +
                            "\n" +
                            "env.execute()\n", out);

            System.out.println(out.toString());

            System.out.println(result.code());
            interpreter.close();
        } catch (Throwable e) {
            e.printStackTrace();
        }

    }


}
