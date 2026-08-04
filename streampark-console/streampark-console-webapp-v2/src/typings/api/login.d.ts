/// <reference path="../global.d.ts"/>

namespace Api {
    namespace Login {
        /** Login payload extends the user entity; some fields may override table defaults. */
        interface Info extends Entity.User {
            id: number
            role: Entity.RoleType[]
            accessToken: string
            refreshToken: string
        }
    }
}
