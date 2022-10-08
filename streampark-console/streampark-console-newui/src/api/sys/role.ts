import { defHttp } from '/@/utils/http/axios';
import { ContentTypeEnum } from '/@/enums/httpEnum';

enum Api {
  RoleUserList = '/role/listByUser',
  AddRole = '/role/post',
  UpdateRole = '/role/update',
  DeleteRole = '/role/delete',
}

export function getRoleListByUser(params?) {
  return defHttp.post({ url: Api.RoleUserList, params });
}

export function addRole(params?) {
  return defHttp.post({
    url: Api.AddRole,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

export function editRole(params?) {
  return defHttp.put({
    url: Api.UpdateRole,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

export function deleteRole(params?) {
  return defHttp.delete({
    url: Api.DeleteRole,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
