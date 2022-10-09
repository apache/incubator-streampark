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
import { NoticyList } from './model/notifyModel';
import { ContentTypeEnum } from '/@/enums/httpEnum';
import { defHttp } from '/@/utils/http/axios';

enum NOTIFY_API {
  NOTICE = '/metrics/notice',
  DEL = '/metrics/delnotice',
}
/**
 * 获取通知列表
 * @param {number} type 通知类型 1:异常告警 2:通知消息,
 * @param {number} pageNum 页码
 * @param {number} pageSize 页大小
 * @returns Promise<NoticyList>
 */
export const fetchNotify = (params: { type: number; pageNum: number; pageSize: number }) => {
  return defHttp.post<NoticyList>({
    url: NOTIFY_API.NOTICE,
    params,
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
};

/**
 * 删除通知
 * @param {number} id 通知id,
 * @returns Promise<boolean>
 */
export const fetchNotifyDelete = (id: string) => {
  return defHttp.post<NoticyList>({
    url: NOTIFY_API.DEL,
    params: { id },
    headers: {
      'Content-Type': ContentTypeEnum.FORM_URLENCODED,
    },
  });
};
