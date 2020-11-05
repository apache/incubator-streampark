package com.streamxhub.repl.flink.interpreter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author benjobs
 */
public class InterpreterOutStream extends OutputStream {

    private final int NEW_LINE_CHAR = '\n';

    ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    private ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();

    private final FlushListener flushListener;

    public InterpreterOutStream(FlushListener listener) {
        this.flushListener = listener;
    }

    /**
     *
     */
    public void clear() {
        buffer.reset();
    }

    @Override
    public void write(int b) {
        buffer.write(b);
        lineBuffer.write(b);
        if (b == NEW_LINE_CHAR) {
            // clear the output on gui
            if (flushListener != null) {
                flushListener.onLine(new String(lineBuffer.toByteArray()));
                lineBuffer.reset();
            }
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        for (int i = off; i < len; i++) {
            write(b[i]);
        }
    }

    public byte[] toByteArray() {
        return buffer.toByteArray();
    }

    @Override
    public void close() throws IOException {
        //最后一行..
        flushListener.onLine(new String(lineBuffer.toByteArray()));
        flush();
    }

    @Override
    public String toString() {
        return new String(toByteArray());
    }

    /**
     * @author benjobs
     */
    public interface FlushListener {
        /**
         * 新的一行
         *
         * @param line
         */
        void onLine(String line);
    }

}


