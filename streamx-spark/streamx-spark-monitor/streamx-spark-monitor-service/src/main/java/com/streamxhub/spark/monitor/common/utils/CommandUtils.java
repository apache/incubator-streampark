/**
 * Copyright (c) 2015 The JobX Project
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

package com.streamxhub.spark.monitor.common.utils;


import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;


public abstract class CommandUtils implements Serializable {

    private static final long serialVersionUID = 6458428317155311192L;

    private static Logger logger = LoggerFactory.getLogger(CommandUtils.class);

    private static String DEFAULT_USER = "root";

    private static String BASH_SCHEAM = "#!/bin/bash";

    public static String executeShell(File shellFile, String... args) {
        String info = null;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            StringBuilder params = new StringBuilder(" ");
            if (args != null && args.length > 0) {
                for (String p : args) {
                    params.append(p).append(" ");
                }
            }
            String line = "/bin/bash +x " + shellFile.getAbsolutePath() + params;
            CommandLine commandLine = CommandLine.parse(line);
            DefaultExecutor exec = new DefaultExecutor();
            exec.setExitValues(null);
            PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream, outputStream);
            exec.setStreamHandler(streamHandler);
            exec.execute(commandLine);
            info = outputStream.toString().trim();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                outputStream.flush();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return info;
        }
    }


    public static int executeScript(String script) {
        assert script != null;
        ProcessBuilder builder = new ProcessBuilder(getCommandLine(script));
        builder.directory(new File(IOUtils.getTmpdir()));
        builder.redirectErrorStream(true);

        int exitCode = -1;
        Process process = null;
        CountDownLatch startupLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(1);

        try {
            process = builder.start();
            int processId = getPID(process);
            if (processId == 0) {
                logger.debug("[JobX]Spawned thread with unknown process id");
            } else {
                logger.debug("[JobX]Spawned thread with process id " + processId);
            }
            startupLatch.countDown();
            try {
                exitCode = process.waitFor();
            } catch (InterruptedException e) {
                logger.info("[JobX]Process interrupted. Exit code is " + exitCode, e);
            }

            completeLatch.countDown();

            String output = new StringBuilder()
                    .append("Stdout:\n")
                    .append(StringUtils.join(IOUtils.readLines(process.getInputStream()), IOUtils.LINE_SEPARATOR_UNIX))
                    .append("\n\n")
                    .append("Stderr:\n")
                    .append(StringUtils.join(IOUtils.readLines(process.getErrorStream()), IOUtils.LINE_SEPARATOR_UNIX))
                    .append("\n")
                    .toString();

            logger.info("[JobX] executeScript,cmd:{},resulr:{}", script, output);

        } catch (Exception e) {
            logger.error("[JobX] executeScript,error:{}", e.getMessage());
        } finally {
            if (process != null) {
                IOUtils.closeQuietly(process.getInputStream());
                IOUtils.closeQuietly(process.getOutputStream());
                IOUtils.closeQuietly(process.getErrorStream());
            }
            return exitCode;
        }
    }

    public static Field getField(Class<?> clazz, String name) throws NoSuchFieldException {
        assert clazz != null;
        for (Class<?> cls = clazz; cls != null; cls = cls.getSuperclass()) {
            try {
                Field field = cls.getDeclaredField(name);
                if (field != null) {
                    field.setAccessible(true);
                }
                return field;
            } catch (Throwable ignored) {
            }
        }
        throw new NoSuchFieldException(clazz.getName() + "#" + name);
    }

    public static int getPID(Process process) {
        int processId = 0;
        try {
            Field field = getField(process.getClass(), "pid");
            processId = field.getInt(process);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return processId;
    }

    public static Integer getPIDByPPID(Integer ppid) {
        if (ppid == null || ppid == 0) {
            return -1;
        }
        try {
            String cmd = String.format("ps -ef|awk '{if($3~/%d/) print $2}'", ppid);
            return CommonUtils.toInt(execute(cmd), 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String execute(String command) {
        Process process = null;
        StringBuffer buffer = new StringBuffer();
        try {
            process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line).append("\n");
            }
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (process != null) {
            try {
                process.getErrorStream().close();
                process.getInputStream().close();
                process.getOutputStream().close();
            } catch (Exception ee) {
            }
        }
        return buffer.toString();
    }

    public static List<String> getCommandLine(String command) {
        ArrayList<String> commands = new ArrayList<String>();
        int index = 0;

        StringBuffer buffer = new StringBuffer(command.length());

        boolean isApos = false;
        boolean isQuote = false;
        while (index < command.length()) {
            char c = command.charAt(index);
            switch (c) {
                case ' ':
                    if (!isQuote && !isApos) {
                        String arg = buffer.toString();
                        buffer = new StringBuffer(command.length() - index);
                        if (arg.length() > 0) {
                            commands.add(arg);
                        }
                    } else {
                        buffer.append(c);
                    }
                    break;
                case '\'':
                    if (!isQuote) {
                        isApos = !isApos;
                    } else {
                        buffer.append(c);
                    }
                    break;
                case '"':
                    if (!isApos) {
                        isQuote = !isQuote;
                    } else {
                        buffer.append(c);
                    }
                    break;
                default:
                    buffer.append(c);
            }

            index++;
        }

        if (buffer.length() > 0) {
            String arg = buffer.toString();
            commands.add(arg);
        }

        return commands;
    }


    public static void write(File shellFile, String command) {
        try {
            if (!shellFile.exists()) {
                PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(shellFile)));
                out.write(BASH_SCHEAM);
                out.write("\n\n");
                out.write(command);
                out.flush();
                out.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }
    }

    public static int chown(boolean r, String user, String group, File file) throws IOException, InterruptedException {
        return runAsExecUser(DEFAULT_USER, String.format("chown %s %s:%s %s", (r ? "-R" : ""), user, group, file.getAbsolutePath()));
    }

    public static int runAsExecUser(final String execUser, final String command) throws IOException, InterruptedException {
        String execCmd = "sudo".concat(IOUtils.BLANK_CHAR).concat(execUser).concat(IOUtils.BLANK_CHAR).concat(command);
        final Process process = Runtime.getRuntime().exec(execCmd);
        return process.waitFor();
    }

}




