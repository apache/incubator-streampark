package com.streamxhub.console.base.properties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShiroProperties {

    private String anonUrl;

    /**
     * token默认有效时间 1天
     */
    private Long jwtTimeOut = 86400L;

}
