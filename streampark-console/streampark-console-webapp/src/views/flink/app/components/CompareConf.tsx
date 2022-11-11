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

import { computed, defineComponent, unref } from 'vue';
import { Input, Select } from 'ant-design-vue';
import { renderCompareSelectTag } from '../hooks/useFlinkRender';
import { Button } from '/@/components/Button';
import Icon from '/@/components/Icon';
import { decodeByBase64 } from '/@/utils/cipher';
import { fetchGetVer } from '/@/api/flink/config';
import { useDrawer } from '/@/components/Drawer';
import Different from './AppDetail/Different.vue';

export default defineComponent({
  name: 'CompareConf',
  props: {
    value: {
      type: Array as PropType<Array<string>>,
      required: true,
    },
    configVersions: {
      type: Array as PropType<Array<any>>,
      required: true,
    },
  },
  emit: ['update:value'],
  setup(props, { emit }) {
    const compareDisabled = computed(() => {
      return props.value.length != 2;
    });
    const [registerDifferentDrawer, { openDrawer: openDiffDrawer }] = useDrawer();

    async function handleCompactConf() {
      const source = await fetchGetVer({
        id: props.value[0],
      });
      const sourceConf = decodeByBase64(source.content);
      const sourceVersion = source.version;
      const target = await fetchGetVer({
        id: props.value[1],
      });
      const targetConf = decodeByBase64(target.content);
      const targetVersion = target.version;
      openDiffDrawer(true, {
        immediate: true,
        param: [
          {
            name: 'Configuration',
            format: 'yaml',
            original: sourceConf,
            modified: targetConf,
          },
        ],
        original: sourceVersion,
        modified: targetVersion,
      });
    }
    return () => {
      return (
        <div>
          <Input.Group compact>
            <Select
              style="width: calc(100% - 60px)"
              placeholder="Please select the configuration version to compare"
              mode="multiple"
              value={props.value}
              onChange={(value) => emit('update:value', value)}
              class="version-select"
              maxTagCount={2}
            >
              {unref(props.configVersions).map((ver) => {
                return <Select.Option key={ver.id}>{renderCompareSelectTag(ver)}</Select.Option>;
              })}
            </Select>
            <Button
              type={unref(compareDisabled) ? 'default' : 'primary'}
              disabled={unref(compareDisabled)}
              class="ml-10px w-50px"
              style={{ color: unref(compareDisabled) ? null : '#fff' }}
              onClick={() => handleCompactConf()}
            >
              <Icon icon="ant-design:swap-outlined" />
            </Button>
          </Input.Group>
          <Different onRegister={registerDifferentDrawer} />
        </div>
      );
    };
  },
});
