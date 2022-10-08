import { SystemSetting } from './types/setting.type';
import { ContentTypeEnum } from '/@/enums/httpEnum';
import { defHttp } from '/@/utils/http/axios';

enum SETTING_APi {
  GET = '/flink/setting/get',
  WEB_URL = '/flink/setting/weburl',
  ALL = '/flink/setting/all',
  CHECK_HADOOP = '/flink/setting/checkHadoop',
  SYNC = '/flink/setting/sync',
  UPDATE = '/flink/setting/update',
}
/**
 * 获取系统设置
 * @returns Promise<MavenSetting[]>
 */
export function fetchSystemSetting() {
  return defHttp.post<SystemSetting[]>({
    url: SETTING_APi.ALL,
  });
}

/**
 * 更新系统设置
 * @param {String} settingKey key
 * @param {Boolean} settingValue value
 * @returns Promise<boolean>
 */
export function fetchSystemSettingUpdate(params: { settingKey: string; settingValue: boolean }) {
  return defHttp.post<boolean>({
    url: SETTING_APi.UPDATE,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
/**
 * 检查配置
 * @returns Promise<boolean>
 */
export function fetchCheckHadoop() {
  return defHttp.post<boolean>({
    url: SETTING_APi.CHECK_HADOOP,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
}
