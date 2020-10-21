/*
 * Copyright (c) 2018 Uber Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.uber.profiling.reporters;

import com.uber.profiling.Reporter;
import com.uber.profiling.ArgumentUtils;
import com.uber.profiling.util.AgentLogger;
import com.uber.profiling.util.JsonUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FileOutputReporter implements Reporter {
    public final static String ARG_OUTPUT_DIR = "outputDir";
    
    private static final AgentLogger logger = AgentLogger.getLogger(FileOutputReporter.class.getName());
    
    private String directory;
    private ConcurrentHashMap<String, FileWriter> fileWriters = new ConcurrentHashMap<>();
    private volatile boolean closed = false;
    
    public FileOutputReporter() {
    }

    public String getDirectory() {
        return directory;
    }

    // This method sets the output directory. By default, this reporter will create a temporary directory
    // and use it as output directory. User could set the output director if want to use another one. But
    // the output directory can only be set at mose once. Setting it again will throw exception.
    public void setDirectory(String directory) {
        synchronized (this) {
            if (this.directory == null || this.directory.isEmpty()) {
                Path path = Paths.get(directory);
                try {
                    if (!Files.exists(path)) {
                        Files.createDirectory(path);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to create directory: " + path, e);
                }

                this.directory = directory;
            } else {
                throw new RuntimeException(String.format("Cannot set directory to %s because it is already has value %s", directory, this.directory));
            }
        }
    }

    @Override
    public void updateArguments(Map<String, List<String>> parsedArgs) {
        String argValue = ArgumentUtils.getArgumentSingleValue(parsedArgs, ARG_OUTPUT_DIR);
        if (ArgumentUtils.needToUpdateArg(argValue)) {
            setDirectory(argValue);
            logger.info("Got argument value for outputDir: " + argValue);
        }
    }

    @Override
    public synchronized void report(String profilerName, Map<String, Object> metrics) {
        if (closed) {
            logger.info("Report already closed, do not report metrics");
            return;
        }
        
        FileWriter writer = ensureFile(profilerName);
        try {
            writer.write(JsonUtils.serialize(metrics));
            writer.write(System.lineSeparator());
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void close() {
        closed = true;
        
        List<FileWriter> copy = new ArrayList<>(fileWriters.values());
        for (FileWriter entry : copy) {
            try {
                entry.flush();
                entry.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private FileWriter ensureFile(String profilerName) {
        synchronized (this) {
            if (directory == null || directory.isEmpty()) {
                try {
                    directory = Files.createTempDirectory("jvm_profiler_").toString();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return fileWriters.computeIfAbsent(profilerName, t -> createFileWriter(t));
    }
    
    private FileWriter createFileWriter(String profilerName) {
        String path = Paths.get(directory, profilerName + ".json").toString();
        try {
            return new FileWriter(path, true);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create file writer: " + path, e);
        }
    }
}
