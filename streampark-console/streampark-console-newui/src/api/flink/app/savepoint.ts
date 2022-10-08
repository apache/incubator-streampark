import { ContentTypeEnum } from '/@/enums/httpEnum';
import { defHttp } from '/@/utils/http/axios';

enum SAVE_POINT_API {
  LATEST = '/flink/savepoint/latest',
  HISTORY = '/flink/savepoint/history',
  DELETE = '/flink/savepoint/delete',
}

export function fetchLatest(params) {
  return defHttp.post({
    url: SAVE_POINT_API.LATEST,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
export function fetchSavePonitHistory(params) {
  return defHttp.post({
    url: SAVE_POINT_API.HISTORY,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
/**
 * 删除
 * @param params id
 * @returns
 */
export function fetchRemoveSavePoint(params: { id: string }) {
  return defHttp.post<boolean>({
    url: SAVE_POINT_API.DELETE,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
