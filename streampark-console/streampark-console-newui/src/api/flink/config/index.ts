import { HadoopConf } from './index.type';
import { ContentTypeEnum } from '/@/enums/httpEnum';
import { defHttp } from '/@/utils/http/axios';

enum CONFIG_API {
  GET = '/flink/conf/get',
  TEMPLATE = '/flink/conf/template',
  LIST = '/flink/conf/list',
  HISTORY = '/flink/conf/history',
  DELETE = '/flink/conf/delete',
  SYS_HADOOP_CONF = '/flink/conf/sysHadoopConf',
}

export function fetchGetVer(params: { id: string }) {
  return defHttp.post({
    url: CONFIG_API.GET,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
export function handleConfTemplate() {
  return defHttp.post<string>({
    url: CONFIG_API.TEMPLATE,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
export function fetchSysHadoopConf() {
  return defHttp.post<HadoopConf>({
    url: CONFIG_API.SYS_HADOOP_CONF,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
export function fetchListVer(params) {
  return defHttp.post({
    url: CONFIG_API.LIST,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
/**
 * 删除配置
 * @param {String}
 * @returns {Promise<Boolean>}
 */
export function fetchRemoveConf(params: { id: string }) {
  return defHttp.post<boolean>({
    url: CONFIG_API.DELETE,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

export function fetchConfHistory(params) {
  return defHttp.post({
    url: CONFIG_API.HISTORY,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
