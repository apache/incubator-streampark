package com.streamxhub.console.core.enums;

/**
 * @author benjobs
 */

public enum ApplicationType {
    STREAMX_FLINK(1, "StreamX Flink"),
    APACHE_FLINK(2, "Apache Flink"),
    STREAMX_SPARK(3, "StreamX Spark"),
    APACHE_SPARK(4, "Apache Spark");
    int type;
    String name;

    ApplicationType(int type, String name) {
        this.type = type;
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public static ApplicationType of(int type) {
        for (ApplicationType etype : ApplicationType.values()) {
            if (etype.getType() == type) {
                return etype;
            }
        }
        return null;
    }


}
