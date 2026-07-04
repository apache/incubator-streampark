/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { AlertTypeEnum } from '@/enums/flinkEnum'

export { AlertTypeEnum }

export interface AlertTypeItem {
  name: string
  value: number
  disabled: boolean
  icon: string
}

export type AlertTypeMap = Record<string, AlertTypeItem>

export function computeAlertType(level: number | null | undefined): string[] {
  let value = level ?? 0
  const result: string[] = []
  while (value !== 0) {
    const code = value & -value
    result.push(String(code))
    value ^= code
  }
  return result
}

export function alertTypes(t: (key: string) => string): AlertTypeMap {
  return {
    [String(AlertTypeEnum.MAIL)]: {
      name: t('setting.alarm.email'),
      value: AlertTypeEnum.MAIL,
      disabled: false,
      icon: 'mail',
    },
    [String(AlertTypeEnum.DINGTALK)]: {
      name: t('setting.alarm.dingTalk'),
      value: AlertTypeEnum.DINGTALK,
      disabled: false,
      icon: 'dingtalk',
    },
    [String(AlertTypeEnum.WECOM)]: {
      name: t('setting.alarm.weChat'),
      value: AlertTypeEnum.WECOM,
      disabled: false,
      icon: 'wecom',
    },
    [String(AlertTypeEnum.MESSAGE)]: {
      name: t('setting.alarm.sms'),
      value: AlertTypeEnum.MESSAGE,
      disabled: true,
      icon: 'message',
    },
    [String(AlertTypeEnum.LARK)]: {
      name: t('setting.alarm.lark'),
      value: AlertTypeEnum.LARK,
      disabled: false,
      icon: 'lark',
    },
  }
}
