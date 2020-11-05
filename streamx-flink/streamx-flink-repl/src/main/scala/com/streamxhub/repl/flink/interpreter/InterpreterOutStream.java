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

    private final InterpreterOutListener flushListener;

    public InterpreterOutStream(InterpreterOutListener listener) {
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
        flush();
    }

    @Override
    public String toString() {
        return new String(toByteArray());
    }
}
