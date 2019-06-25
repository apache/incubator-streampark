package com.streamxhub.spark.monitor.common.exception;

/**
 * STREAMX 系统内部异常
 */
public class StreamXException extends Exception {

    private static final long serialVersionUID = -994962710559017255L;

    public StreamXException(String message) {
        super(message);
    }
}
