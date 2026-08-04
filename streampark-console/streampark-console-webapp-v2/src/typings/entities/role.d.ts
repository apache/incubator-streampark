/// <reference path="../global.d.ts"/>

namespace Entity {
    type RoleType = 'super' | 'admin' | 'user'

    interface Role {
        id?: number
        role?: RoleType
    }
}
