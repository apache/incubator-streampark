import { AxiosResponse } from 'axios';
import { ContentTypeEnum } from '/@/enums/httpEnum';
import { defHttp } from '/@/utils/http/axios';

enum FLINK_SQL_API {
  VERIFY = '/flink/sql/verify',
  GET = '/flink/sql/get',
  HISTORY = '/flink/sql/history',
}

export function fetchFlinkSqlVerify(params) {
  return defHttp.post<AxiosResponse<any>>(
    {
      url: FLINK_SQL_API.VERIFY,
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

export function fetchFlinkSql(params) {
  return defHttp.post({
    url: FLINK_SQL_API.GET,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

export function fetchFlinkHistory(params) {
  return defHttp.post({
    url: FLINK_SQL_API.HISTORY,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
