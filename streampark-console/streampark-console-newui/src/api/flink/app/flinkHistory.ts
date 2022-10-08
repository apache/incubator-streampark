import { ContentTypeEnum } from '/@/enums/httpEnum';
import { defHttp } from '/@/utils/http/axios';

enum HISTORY_API {
  UPLOAD_JARS = '/flink/history/uploadJars',
  K8S_NAMESPACES = '/flink/history/k8sNamespaces',
  SESSION_CLUSTER_IDS = '/flink/history/sessionClusterIds',
  FLINK_BASE_IMAGES = '/flink/history/flinkBaseImages',
  FLINK_POD_TEMPLATES = '/flink/history/flinkPodTemplates',
  FLINK_JM_POD_TEMPLATES = '/flink/history/flinkJmPodTemplates',
  FLINK_TM_POD_TEMPLATES = '/flink/history/flinkTmPodTemplates',
}

/**
 * 获取k8s
 * @returns Promise<any>
 */
export function fetchK8sNamespaces() {
  return defHttp.post<string[]>({
    url: HISTORY_API.K8S_NAMESPACES,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

/**
 * 上传jar
 * @returns Promise<DashboardResponse>
 */
export function fetchUploadJars() {
  return defHttp.post<string[]>({
    url: HISTORY_API.UPLOAD_JARS,
  });
}

export function fetchSessionClusterIds(params) {
  return defHttp.post({
    url: HISTORY_API.SESSION_CLUSTER_IDS,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

export function fetchFlinkBaseImages() {
  return defHttp.post({
    url: HISTORY_API.FLINK_BASE_IMAGES,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

export function fetchFlinkPodTemplates(params) {
  return defHttp.post<string[]>({
    url: HISTORY_API.FLINK_POD_TEMPLATES,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

export function fetchFlinkJmPodTemplates(params) {
  return defHttp.post<string[]>({
    url: HISTORY_API.FLINK_JM_POD_TEMPLATES,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}

export function fetchFlinkTmPodTemplates(params) {
  return defHttp.post<string[]>({
    url: HISTORY_API.FLINK_TM_POD_TEMPLATES,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
