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
