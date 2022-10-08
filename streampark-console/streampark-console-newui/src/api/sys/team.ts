import { defHttp } from '/@/utils/http/axios';
import { ContentTypeEnum } from '/@/enums/httpEnum';

enum Api {
  TeamListByUser = '/team/listByUser',
  TeamList = '/team/list',
  AddTeam = '/team/post',
  DeleteTeam = '/team/delete',
}

export function getTeamListByUser(params?) {
  return defHttp.post({ url: Api.TeamListByUser, params });
}

export function getTeamList(params?) {
  return defHttp.post({
    url: Api.TeamList,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

export function addTeam(params) {
  return defHttp.post({
    url: Api.AddTeam,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

export function deleteTeam(params) {
  return defHttp.delete({
    url: Api.DeleteTeam,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
