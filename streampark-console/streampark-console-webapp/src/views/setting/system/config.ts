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
import { FormSchema } from '/@/components/Form';
import { useI18n } from '/@/hooks/web/useI18n';

export type SettingType = 'docker' | 'email';

type SettingForm = {
  [P in SettingType]: FormSchema[];
};

const { t } = useI18n();
export const settingFormSchema: SettingForm = {
  docker: [
    {
      field: 'address',
      label: t('setting.system.docker.address.label'),
      helpMessage: t('setting.system.docker.address.desc'),
      component: 'Input',
      componentProps: {
        placeholder: t('setting.system.docker.address.label'),
      },
      required: true,
    },
    {
      field: 'namespace',
      label: t('setting.system.docker.namespace.label'),
      helpMessage: t('setting.system.docker.namespace.desc'),
      component: 'Input',
      componentProps: {
        placeholder: t('setting.system.docker.namespace.label'),
      },
      required: true,
    },
    {
      field: 'username',
      label: t('setting.system.docker.username.label'),
      helpMessage: t('setting.system.docker.username.desc'),
      component: 'Input',
      componentProps: {
        placeholder: t('setting.system.docker.username.label'),
      },
      required: true,
    },
    {
      field: 'password',
      label: t('setting.system.docker.password.label'),
      helpMessage: t('setting.system.docker.password.desc'),
      component: 'InputPassword',
      componentProps: {
        autocomplete: 'new-password',
        placeholder: t('setting.system.docker.password.label'),
      },
      required: true,
    },
  ],
  email: [
    {
      field: 'host',
      label: t('setting.system.email.host.label'),
      helpMessage: t('setting.system.email.host.desc'),
      component: 'Input',
      componentProps: {
        placeholder: t('setting.system.email.host.label'),
      },
      required: true,
    },
    {
      field: 'port',
      label: t('setting.system.email.port.label'),
      helpMessage: t('setting.system.email.port.desc'),
      component: 'InputNumber',
      componentProps: {
        class: '!w-full',
        controls: false,
        min: 0,
        max: 65535,
        placeholder: t('setting.system.email.port.label'),
      },
      required: true,
    },
    {
      field: 'from',
      label: t('setting.system.email.from.label'),
      helpMessage: t('setting.system.email.from.desc'),
      component: 'Input',
      componentProps: {
        placeholder: t('setting.system.email.from.label'),
      },
      required: true,
    },
    {
      field: 'userName',
      label: t('setting.system.email.userName.label'),
      helpMessage: t('setting.system.email.userName.desc'),
      component: 'Input',
      componentProps: {
        placeholder: t('setting.system.email.userName.label'),
      },
      required: true,
    },
    {
      field: 'password',
      label: t('setting.system.email.password.label'),
      helpMessage: t('setting.system.email.password.label'),
      component: 'InputPassword',
      componentProps: {
        autocomplete: 'new-password',
        placeholder: t('setting.system.email.password.label'),
      },
      required: true,
    },
    {
      field: 'ssl',
      label: t('setting.system.email.ssl.label'),
      helpMessage: t('setting.system.email.ssl.label'),
      component: 'Switch',
      componentProps: {
        checkedChildren: 'ON',
        unCheckedChildren: 'OFF',
      },
    },
  ],
};
