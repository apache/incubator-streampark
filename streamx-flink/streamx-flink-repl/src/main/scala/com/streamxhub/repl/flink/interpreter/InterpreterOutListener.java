package com.streamxhub.repl.flink.interpreter;

/**
 * @author benjobs
 */
public interface InterpreterOutListener {
    /**
     * 新的一行
     * @param line
     */
    void onLine(String line);
}
