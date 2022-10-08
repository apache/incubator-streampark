export interface SystemSetting {
  orderNum: number;
  settingName: string;
  settingKey: string;
  settingValue?: any;
  type: number;
  description: string;
  editable: boolean;
  submitting: boolean;
  flinkHome?: any;
  flinkConf?: any;
}
