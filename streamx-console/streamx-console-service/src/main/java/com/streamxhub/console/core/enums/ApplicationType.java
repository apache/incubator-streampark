package com.streamxhub.console.core.enums;

/**
 * @author benjobs
 */

public enum ApplicationType {
    STREAMX_FLINK(1),
    APACHE_FLINK(2),
    STREAMX_SPARK(3),
    APACHE_SPARK(4);
    int type;

    ApplicationType(int type) {
        this.type = type;
    }

    public int get() {
        return type;
    }

    public static ApplicationType of(int type) {
        for (ApplicationType etype : ApplicationType.values()) {
            if (etype.get() == type) {
                return etype;
            }
        }
        return null;
    }


}
