/// <reference path="../global.d.ts"/>

/** 用户数据库表字段 */
namespace Entity {
  interface User {
    /** 用户id */
    id?: number
    /** 用户名 */
    userName?: string
    /* 用户头像 */
    avatar?: string
    /* 用户性别 */
    gender?: 0 | 1
    /* 用户邮箱 */
    email?: string
    /* 用户昵称 */
    nickname?: string
    /* 用户电话 */
    tel?: string
    /** 用户角色类型 */
    role?: Entity.RoleType[]
    /** 用户状态 */
    status?: 0 | 1
    /** 备注 */
    remark?: string
  }

}
