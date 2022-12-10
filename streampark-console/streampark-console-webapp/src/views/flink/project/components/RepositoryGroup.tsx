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
import { defineComponent } from 'vue';
import type { PropType } from 'vue';
import { Form, Input, Select } from 'ant-design-vue';
export interface RepositoryProps {
  gitProtocol: string | number;
  url: string;
}
import { useI18n } from '/@/hooks/web/useI18n';
import { GitProtocolEnum } from '/@/enums/projectEnum';
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
      { label: 'http/https', value: GitProtocolEnum.HTTPS },
      { label: 'ssh', value: GitProtocolEnum.SSH },
    ];

    return () => {
      return (
        <div>
          <Input.Group compact class="!flex custom-input-group">
            <Select
              name="gitProtocol"
              style="width: 120px"
              placeholder={t('flink.project.form.gitProtocolPlaceholder')}
              value={props.value?.gitProtocol}
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
        </div>
      );
    };
  },
});
