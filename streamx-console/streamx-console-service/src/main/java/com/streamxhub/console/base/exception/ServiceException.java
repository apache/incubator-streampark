package com.streamxhub.console.base.exception;

/**
 * 系统内部异常
 */
public class ServiceException extends Exception {

    private static final long serialVersionUID = -994962710559017255L;

    public ServiceException(String message) {
        super(message);
    }
}
