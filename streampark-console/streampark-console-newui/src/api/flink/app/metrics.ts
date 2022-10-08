import { AxiosResponse } from 'axios';
import { ContentTypeEnum } from '/@/enums/httpEnum';
import { defHttp } from '/@/utils/http/axios';

enum Metrics_API {
  FLAME_GRAPH = '/metrics/flamegraph',
  NOTICE = '/metrics/notice',
  DEL_NOTICE = '/metrics/delnotice',
}
/**
 * 导出
 * @param {String} appId application id
 * @param {Number} width The width of the screen
 * @returns {Promise<AxiosResponse<Blob>>} Promise<AxiosResponse<Blob>>
 */
export function fetchFlamegraph(params) {
  return defHttp.post<AxiosResponse<Blob>>(
    {
      url: Metrics_API.FLAME_GRAPH,
      params,
      headers: {
        'Content-Type': ContentTypeEnum.FORM_URLENCODED,
      },
      responseType: 'blob',
    },
    {
      isReturnNativeResponse: true,
    },
  );
}
