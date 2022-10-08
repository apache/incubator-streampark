import { defHttp } from '/@/utils/http/axios';
import { ContentTypeEnum } from '/@/enums/httpEnum';

enum Api {
  TokenList = '/token/list',
  ToggleTokenStatus = '/token/toggle',
  AddToken = 'token/create',
  DeleteToken = 'token/delete',
  CHECK = 'token/check',
  CURL = '/token/curl',
}

export function getTokenList(params?) {
  return defHttp.post({
    url: Api.TokenList,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

export function setTokenStatus(data) {
  return defHttp.post({
    url: Api.ToggleTokenStatus,
    data,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

export function addToken(data) {
  return defHttp.post({
    url: Api.ToggleTokenStatus,
    data,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
export function deleteToken(data) {
  return defHttp.post({
    url: Api.DeleteToken,
    data,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

/**
 * 检查token
 * @param data
 * @returns {Promise<number>}
 */
export function fetchCheckToken(data) {
  return defHttp.post<number>({
    url: Api.CHECK,
    data,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
/**
 * copyCurl
 * @param data
 * @returns {Promise<string>}
 */
export function fetchCopyCurl(data) {
  return defHttp.post<string>({
    url: Api.CURL,
    data,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
