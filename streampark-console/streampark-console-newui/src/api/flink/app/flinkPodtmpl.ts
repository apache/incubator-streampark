import { ContentTypeEnum } from '/@/enums/httpEnum';
import { defHttp } from '/@/utils/http/axios';

enum PODTEMPLATE_API {
  SYS_HOSTS = '/flink/podtmpl/sysHosts',
  INIT = '/flink/podtmpl/init',
  COMP_HOST_ALIAS = '/flink/podtmpl/compHostAlias',
  EXTRACT_HOST_ALIAS = '/flink/podtmpl/extractHostAlias',
  PREVIEW_HOST_ALIAS = '/flink/podtmpl/previewHostAlias',
}

export function fetchSysHosts(params) {
  return defHttp.post({
    url: PODTEMPLATE_API.SYS_HOSTS,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

export function fetchInitPodTemplate(params) {
  return defHttp.post({
    url: PODTEMPLATE_API.INIT,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

export function fetchCompleteHostAliasToPodTemplate(params) {
  return defHttp.post({
    url: PODTEMPLATE_API.COMP_HOST_ALIAS,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
export function fetchExtractHostAliasFromPodTemplate(params) {
  return defHttp.post({
    url: PODTEMPLATE_API.EXTRACT_HOST_ALIAS,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

export function fetchPreviewHostAlias(params) {
  return defHttp.post({
    url: PODTEMPLATE_API.PREVIEW_HOST_ALIAS,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
