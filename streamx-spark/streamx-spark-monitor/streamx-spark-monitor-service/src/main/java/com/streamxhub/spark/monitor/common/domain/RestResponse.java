package com.streamxhub.spark.monitor.common.domain;

import java.util.HashMap;

/**
 * @author benjobs
 */
public class RestResponse extends HashMap<String, Object> {

    private static final long serialVersionUID = -8713837118340960775L;

    public RestResponse message(String message) {
        this.put("message", message);
        return this;
    }

    public RestResponse data(Object data) {
        this.put("data", data);
        return this;
    }

    @Override
    public RestResponse put(String key, Object value) {
        super.put(key, value);
        return this;
    }
}
