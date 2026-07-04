/// <reference path="../global.d.ts"/>

/* 字典数据库表字段 */
namespace Entity {

  interface Dict {
    id?: number
    isRoot?: 0 | 1
    code: string
    label: string
    value?: number
  }
}
