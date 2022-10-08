export interface AlertSetting {
  id: string;
  userId: string;
  alertName: string;
  alertType: number;
  emailParams: string;
  dingTalkParams: string;
  weComParams: string;
  httpCallbackParams?: any;
  larkParams: string;
  createTime: string;
  modifyTime: string;
}

/* 创建告警 */
export interface AlertCreate {
  id?: any;
  alertName: string;
  userId: string | number;
  alertType: number;
  emailParams: EmailParams;
  dingTalkParams: DingTalkParams;
  weComParams: WeComParams;
  larkParams: LarkParams;
  isJsonType?: boolean;
}

interface LarkParams {
  token: string;
  isAtAll: boolean;
  secretEnable: boolean;
  secretToken: string;
}

interface WeComParams {
  token: string;
}

interface DingTalkParams {
  token?: string;
  contacts?: string;
  alertDingURL?: string;
  secretEnable?: boolean;
  secretToken?: string;
  isAtAll: boolean;
}

interface EmailParams {
  contacts: string;
}
