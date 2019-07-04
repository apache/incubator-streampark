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

import org.apache.commons.codec.digest.DigestUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by benjobs on 15/6/24.
 */
public abstract class IOUtils implements Serializable {

    private static final int EOF = -1;
    /**
     * The Unix directory separator character.
     */
    public static final char DIR_SEPARATOR_UNIX = '/';
    /**
     * The Windows directory separator character.
     */
    public static final char DIR_SEPARATOR_WINDOWS = '\\';
    /**
     * The system directory separator character.
     */
    public static final char DIR_SEPARATOR = File.separatorChar;
    /**
     * The Unix line separator string.
     */
    public static final String LINE_SEPARATOR_UNIX = "\n";
    /**
     * The Windows line separator string.
     */
    public static final String LINE_SEPARATOR_WINDOWS = "\r\n";

    public static final String FIELD_TERMINATED_BY = new String(new char['\001']);

    public static final String TAB = "\t";

    private static final int BUFFER_SIZE = 1024 * 8;

    public static final String BLANK_CHAR = " ";


    public static String readText(File file, String charset) {
        InputStream inputStream = null;
        InputStreamReader inputReader = null;
        BufferedReader bufferReader = null;

        if (file != null && file.exists()) {
            try {
                inputStream = new FileInputStream(file);
                inputReader = new InputStreamReader(new FileInputStream(file), charset);
                bufferReader = new BufferedReader(inputReader);

                StringBuffer strBuffer = new StringBuffer();
                // 读取一行
                String line;
                while ((line = bufferReader.readLine()) != null) {
                    strBuffer.append(line).append("\n\r");
                }
                return strBuffer.toString();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (bufferReader != null) {
                        bufferReader.close();
                    }

                    if (inputReader != null) {
                        inputReader.close();
                    }

                    if (inputStream != null) {
                        inputStream.close();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public static boolean writeText(File file, Serializable text, String charset) {
        try {
            PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), charset));
            out.write(text.toString());
            out.flush();
            out.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void writeFile(File file, InputStream inputStream) throws IOException {
        assert null != file;
        assert null != inputStream;
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file));
        try {
            int r;
            while ((r = inputStream.read()) != -1) {
                output.write((byte) r);
            }

        } finally {
            if (output != null) {
                output.flush();
                output.close();
            }
        }
    }

    public static byte[] toByteArray(File file) throws IOException {
        InputStream input = null;
        try {
            input = openInputStream(file);
            long size = file.length();
            if (file.length() > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Size cannot be greater than Integer max value: " + size);
            }

            if (size < 0) {
                throw new IllegalArgumentException("Size must be equal or greater than zero: " + size);
            }

            if (size == 0) {
                return new byte[0];
            }

            byte[] data = new byte[(int) size];

            int offset = 0;
            int readed;

            while (offset < size && (readed = input.read(data, offset, (int) size - offset)) != EOF) {
                offset += readed;
            }

            if (offset != size) {
                throw new IOException("Unexpected readed size. current: " + offset + ", excepted: " + size);
            }
            return data;
        } finally {
            if (input != null) {
                input.close();
            }
        }
    }

    public static FileInputStream openInputStream(File file) throws IOException {
        if (file.exists()) {
            if (file.isDirectory()) {
                throw new IOException("File '" + file + "' exists but is a directory");
            }
            if (file.canRead() == false) {
                throw new IOException("File '" + file + "' cannot be read");
            }
        } else {
            throw new FileNotFoundException("File '" + file + "' does not exist");
        }
        return new FileInputStream(file);
    }


    public static final String getTmpdir() {
        return System.getProperty("java.io.tmpdir");
    }

    public static final String getProjectFolderPath() {
        String path = null;
        try {
            File directory = new File("");
            path = directory.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return path;
    }

    /**
     * write.
     *
     * @param is InputStream instance.
     * @param os OutputStream instance.
     * @return count.
     */
    public static long write(InputStream is, OutputStream os) throws IOException {
        return write(is, os, BUFFER_SIZE);
    }

    /**
     * write.
     *
     * @param is         InputStream instance.
     * @param os         OutputStream instance.
     * @param bufferSize buffer size.
     * @return count.
     */
    public static long write(InputStream is, OutputStream os, int bufferSize) throws IOException {
        int read;
        long total = 0;
        byte[] buff = new byte[bufferSize];
        while (is.available() > 0) {
            read = is.read(buff, 0, buff.length);
            if (read > 0) {
                os.write(buff, 0, read);
                total += read;
            }
        }
        os.flush();
        return total;
    }

    /**
     * read string.
     *
     * @param reader Reader instance.
     * @return String.
     * @throws IOException
     */
    public static String read(Reader reader) throws IOException {
        StringWriter writer = new StringWriter();
        try {
            write(reader, writer);
            return writer.getBuffer().toString();
        } finally {
            writer.close();
        }
    }

    /**
     * write string.
     *
     * @param writer Writer instance.
     * @param string String.
     * @throws IOException
     */
    public static long write(Writer writer, String string) throws IOException {
        Reader reader = new StringReader(string);
        try {
            return write(reader, writer);
        } finally {
            reader.close();
        }
    }

    /**
     * write.
     *
     * @param reader Reader.
     * @param writer Writer.
     * @return count.
     * @throws IOException
     */
    public static long write(Reader reader, Writer writer) throws IOException {
        return write(reader, writer, BUFFER_SIZE);
    }

    /**
     * write.
     *
     * @param reader     Reader.
     * @param writer     Writer.
     * @param bufferSize buffer size.
     * @return count.
     * @throws IOException
     */
    public static long write(Reader reader, Writer writer, int bufferSize) throws IOException {
        int read;
        long total = 0;
        char[] buf = new char[BUFFER_SIZE];
        while ((read = reader.read(buf)) != -1) {
            writer.write(buf, 0, read);
            total += read;
        }
        writer.flush();
        return total;
    }

    /**
     * read lines.
     *
     * @param file file.
     * @return lines.
     * @throws IOException
     */
    public static String[] readLines(File file) throws IOException {
        if (file == null || !file.exists() || !file.canRead()) {
            return new String[0];
        }
        return readLines(new FileInputStream(file));
    }

    /**
     * read lines.
     *
     * @param is input stream.
     * @return lines.
     * @throws IOException
     */
    public static String[] readLines(InputStream is) throws IOException {
        List<String> lines = new ArrayList<String>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            return lines.toArray(new String[0]);
        }
    }

    /**
     * write lines.
     *
     * @param os    output stream.
     * @param lines lines.
     * @throws IOException
     */
    public static void writeLines(OutputStream os, String[] lines) throws IOException {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(os))) {
            for (String line : lines) {
                writer.println(line);
            }
            writer.flush();
        }
    }

    /**
     * write lines.
     *
     * @param file  file.
     * @param lines lines.
     * @throws IOException
     */
    public static void writeLines(File file, String[] lines) throws IOException {
        if (file == null) {
            throw new IOException("File is null.");
        }
        writeLines(new FileOutputStream(file), lines);
    }

    /**
     * append lines.
     *
     * @param file  file.
     * @param lines lines.
     * @throws IOException
     */
    public static void appendLines(File file, String[] lines) throws IOException {
        if (file == null)
            throw new IOException("File is null.");
        writeLines(new FileOutputStream(file, true), lines);
    }

    public static void main(String[] args) {
        System.out.println(getProjectFolderPath());
    }

    public static boolean fileExists(Object file) {
        assert file != null;
        if (file instanceof String) {
            file = new File((String) file);
        }
        return ((File) file).exists();
    }

    public static String getCurrentPath(Class type) {
        assert type != null;
        return type.getProtectionDomain().getCodeSource().getLocation().getFile();
    }

    public static long getFileSize(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        FileChannel fileChannel = fis.getChannel();
        return fileChannel.size();
    }

    public static String getFileMD5(File file) throws IOException {
        return DigestUtils.md5Hex(toByteArray(file));
    }

    public static void closeQuietly(Reader input) {
        closeQuietly((Closeable) input);
    }

    public static void closeQuietly(Writer output) {
        closeQuietly((Closeable) output);
    }

    public static void closeQuietly(InputStream input) {
        closeQuietly((Closeable) input);
    }

    public static void closeQuietly(OutputStream output) {
        closeQuietly((Closeable) output);
    }

    public static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException var2) {
            ;
        }

    }

    public static void closeQuietly(Socket sock) {
        if (sock != null) {
            try {
                sock.close();
            } catch (IOException var2) {
                ;
            }
        }

    }

    public static void closeQuietly(Selector selector) {
        if (selector != null) {
            try {
                selector.close();
            } catch (IOException var2) {
                ;
            }
        }

    }

    public static void closeQuietly(ServerSocket sock) {
        if (sock != null) {
            try {
                sock.close();
            } catch (IOException var2) {
                ;
            }
        }

    }

}
