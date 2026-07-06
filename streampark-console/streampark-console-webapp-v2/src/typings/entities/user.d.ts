/// <reference path="../global.d.ts"/>

namespace Entity {
  interface User {
    id?: number
    userName?: string
    avatar?: string
    gender?: 0 | 1
    email?: string
    nickname?: string
    tel?: string
    role?: Entity.RoleType[]
    status?: 0 | 1
    remark?: string
  }

}
