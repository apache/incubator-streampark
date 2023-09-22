/* 
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License. 
*/
import { defineComponent, h } from 'vue';
import type { PropType } from 'vue';
import { Form, Input, Select, Tag } from 'ant-design-vue';
export interface RepositoryProps {
  gitCredential: string | number;
  url: string;
}
import { useI18n } from '/@/hooks/web/useI18n';
import { GitCredentialEnum } from '/@/enums/projectEnum';
import { SvgIcon } from '/@/components/Icon';
export default defineComponent({
  name: 'RepositoryUrl',
  props: {
    value: {
      type: Object as PropType<RepositoryProps>,
      required: true,
    },
  },
  emits: ['updateProtocol', 'updateUrl'],
  setup(props, { emit }) {
    const { t } = useI18n();
    const formItemContext = Form.useInjectFormItemContext();
    const handleProtocolChange = (value: any) => {
      emit('updateProtocol', value);
      formItemContext.onFieldChange();
    };
    const handleUrlChange = (value: any) => {
      emit('updateUrl', value);
      formItemContext.onFieldChange();
    };

    const options = [
      {
        label: h('div', {}, [h(SvgIcon, { name: 'http', color: '#108ee9', size: '30' }, '')]),
        value: GitCredentialEnum.HTTPS,
      },
      {
        label: h('div', {}, [h(SvgIcon, { name: 'ssh', color: '#108ee9', size: '30' }, '')]),
        value: GitCredentialEnum.SSH,
      },
    ];

    return () => {
      return (
        <div>
          <Input.Group compact class="!flex custom-input-group">
            <Select
              name="gitCredential"
              style="width: 80px"
              placeholder={t('flink.project.form.gitCredentialPlaceholder')}
              value={props.value?.gitCredential}
              onChange={(e: any) => handleProtocolChange(e)}
              options={options}
            />
            <Input
              name="url"
              placeholder={t('flink.project.form.repositoryURLPlaceholder')}
              allow-clear
              class="flex-1"
              value={props.value?.url}
              onInput={(e: any) => handleUrlChange(e.target.value)}
            />
          </Input.Group>
          <p class="conf-desc mt-10px">
            <span class="note-info">
              <Tag color="#2db7f5" class="tag-note">
                {t('flink.app.noteInfo.note')}
              </Tag>
              {props.value?.gitCredential === 1 && (
                <span>{t('flink.project.operationTips.httpsCredential')}</span>
              )}
              {props.value?.gitCredential === 2 && (
                <span>{t('flink.project.operationTips.sshCredential')}</span>
              )}
            </span>
          </p>
        </div>
      );
    };
  },
});
