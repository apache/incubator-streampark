package com.streamxhub.streamx.console.base.domain.router;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Vue路由 Meta
 *
 * @author benjobs
 */
@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RouterMeta implements Serializable {

    private static final long serialVersionUID = 5499925008927195914L;

    private Boolean closeable;

    private Boolean hidden;

    private Boolean keepAlive;

    // private Boolean hideHeader;

    // private Boolean hiddenHeaderContent;

    private String icon;
}
