import { defHttp } from '/@/utils/http/axios';
import { getMenuListResultModel } from './model/menuModel';
import { ContentTypeEnum } from '/@/enums/httpEnum';

enum Api {
  GetMenuList = '/getMenuList',
  MenuRouter = '/menu/router',
  AddMenu = '/menu/post',
  UpdateMenu = '/menu/update',
}

/**
 * @description: Get user menu based on id
 */

export const getMenuList = () => {
  return defHttp.get<getMenuListResultModel>({ url: Api.GetMenuList });
};

export const getMenuRouter = () => {
  return defHttp.post<getMenuListResultModel>({ url: Api.MenuRouter });
};

export function editMenu(params?) {
  return defHttp.put({
    url: Api.UpdateMenu,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

export function addMenu(params?) {
  return defHttp.post({
    url: Api.AddMenu,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
