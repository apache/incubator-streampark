import { AxiosResponse } from 'axios';
import { ContentTypeEnum } from '/@/enums/httpEnum';
import { defHttp } from '/@/utils/http/axios';

enum BUILD_API {
  BUILD = '/flink/pipe/build',
  DETAIL = '/flink/pipe/detail',
}

export function fetchBuild(params) {
  return defHttp.post<AxiosResponse<any>>(
    {
      url: BUILD_API.BUILD,
      params,
      headers: {
        'Content-Type': ContentTypeEnum.FORM_URLENCODED,
      },
    },
    {
      isReturnNativeResponse: true,
    },
  );
}
/**
 * 获取构建详情
 * @param params appId:string
 * @returns
 */
export function fetchBuildDetail(params: { appId: string }) {
  return defHttp.post<{ pipeline: any; docker: any }>({
    url: BUILD_API.DETAIL,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
