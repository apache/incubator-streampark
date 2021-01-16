package com.streamxhub.streamx.console.system.entity;

import java.io.Serializable;

import lombok.Data;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("t_user_role")
@Data
public class UserRole implements Serializable {

    private static final long serialVersionUID = -3166012934498268403L;

    private Long userId;

    private Long roleId;
}
