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
package com.streamxhub.streamx.repl.flink.test;

import com.streamxhub.streamx.repl.flink.interpreter.FlinkInterpreter;
import com.streamxhub.streamx.repl.flink.interpreter.InterpreterOutput;
import com.streamxhub.streamx.repl.flink.interpreter.InterpreterResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;


public class FlinkInterpreterTest {

    private FlinkInterpreter interpreter;

    private String flinkHome = System.getenv("FLINK_HOME");

    @Before
    public void setUp() {
        Properties p = new Properties();
        p.setProperty("repl.out", "true");
        p.setProperty("scala.color", "false");
        p.setProperty("flink.execution.mode", "local");
        p.setProperty("local.number-taskmanager", "4");
        interpreter = new FlinkInterpreter(p);
    }

    @After
    public void testDown() throws Exception {
        if (interpreter != null) {
            interpreter.close();
        }
    }

    @Test
    public void testWordCount() {
        try {
            InterpreterOutput out = new InterpreterOutput(line -> {

            });

            interpreter.open(flinkHome);

            InterpreterResult result = interpreter.interpret(
                    "val data = env.fromElements(\"hello world\", \"hello flink\", \"hello hadoop\")\n" +
                            "\n" +
                            "data.flatMap(line => line.split(\"\\\\s\"))\n" +
                            "  .map(x=>(x,1))\n" +
                            "  .keyBy(0)\n" +
                            "  .sum(1)\n" +
                            "  .print()\n" +
                            "\n" +
                            "env.execute()\n", out);

            System.out.println(out);

            System.out.println(result.code());
            interpreter.close();
        } catch (Throwable e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testStream() {
        String code = "\n" +
                "%flink.execution.mode=yarn\n" +
                "// the host and the port to connect to\n" +
                "val hostname = \"test-hadoop-2\"\n" +
                "val port = 9999\n" +
                "\n" +
                "// get the execution environment\n" +
                "val env = StreamExecutionEnvironment.getExecutionEnvironment\n" +
                "env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime)\n" +
                "\n" +
                "// get input data by connecting to the socket\n" +
                "val text = env.socketTextStream(hostname, port, \"\\n\")\n" +
                "\n" +
                "// parse the data, group it, window it, and aggregate the counts\n" +
                "val windowCounts = text.flatMap(new FlatMapFunction[String,(String,Long)] {\n" +
                "    override def flatMap(value: String, out: Collector[(String, Long)]): Unit = {\n" +
                "       for (word <- value.split(\"\\\\s\")) {\n" +
                "           out.collect(word,1L)\n" +
                "       }\n" +
                "    }\n" +
                "}).keyBy(0).timeWindow(Time.seconds(5)).reduce(new ReduceFunction[(String,Long)]() {\n" +
                "    override def reduce(a: (String, Long), b: (String, Long)): (String, Long) = (a._1,a._2 + b._2)\n" +
                "})\n" +
                "\n" +
                "// print the results with a single thread, rather than in parallel\n" +
                "windowCounts.print.setParallelism(1)\n" +
                "env.execute(\"Socket Window WordCount with StreamX NoteBook\")\n";

        try {
            InterpreterOutput out = new InterpreterOutput(line -> {

            });
            InterpreterResult result = interpreter.interpret(code, out);
            System.out.println(out);
            System.out.println(result.code());
            interpreter.close();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }


}
