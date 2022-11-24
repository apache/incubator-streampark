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
import { Button, Form, Input, InputNumber, Tag, Select } from 'ant-design-vue';
import Icon from '/@/components/Icon';
export interface CheckPointFailure {
  cpMaxFailureInterval: number;
  cpFailureRateInterval: number;
  cpFailureAction: string;
}
import { cpTriggerAction } from '../data';
export default defineComponent({
  props: {
    value: {
      type: Object as PropType<CheckPointFailure>,
      required: true,
    },
  },
  emits: ['updateValue'],
  setup(props, { emit }) {
    const formItemContext = Form.useInjectFormItemContext();
    const triggerChange = (changedValue: Partial<CheckPointFailure>) => {
      emit('updateValue', { ...props.value, ...changedValue });
      formItemContext.onFieldChange();
    };
    const handleCpFailureRateIntervalChange = (value: any) => {
      triggerChange({ cpFailureRateInterval: value });
    };
    const handleCpMaxFailureIntervalChange = (value: any) => {
      // const newNumber = (e.target as any).value;
      triggerChange({ cpMaxFailureInterval: value });
    };
    const handleFailureActionChange = (value: any) => {
      triggerChange({ cpFailureAction: value });
    };
    return () => {
      return (
        <div>
          <Input.Group compact class="!flex">
            <InputNumber
              min={1}
              step={1}
              name="cpMaxFailureInterval"
              placeholder="checkpoint failure rate interval"
              allow-clear
              class="!w-260px mr-10px"
              value={props.value?.cpMaxFailureInterval}
              onChange={(value: any) => handleCpMaxFailureIntervalChange(value)}
            />
            <Button style="width: 70px"> minute </Button>
            <InputNumber
              style="margin-left: 1%"
              name="cpFailureRateInterval"
              min={1}
              step={1}
              placeholder="max failures per interval"
              class="!mb-0 !w-200px"
              value={props.value?.cpFailureRateInterval}
              onChange={(value: any) => handleCpFailureRateIntervalChange(value)}
            />

            <Button style="width: 70px"> count </Button>
            <Select
              name="cpFailureAction"
              style="margin-left: 1%"
              placeholder="trigger action"
              allow-clear
              class="!mb-0 flex-1"
              value={props.value?.cpFailureAction}
              onChange={(e: any) => handleFailureActionChange(e)}
            >
              {cpTriggerAction.map((o) => {
                return (
                  <Select.Option key={o.value}>
                    <Icon
                      icon={
                        o.value === 1 ? 'ant-design:alert-outlined' : 'ant-design:sync-outlined'
                      }
                    />
                    {o.label}
                  </Select.Option>
                );
              })}
            </Select>
          </Input.Group>
          <p class="conf-desc mt-10px">
            <span class="note-info">
              <Tag color="#2db7f5" class="tag-note">
                Note
              </Tag>
              Operation after checkpoint failure, e.g: Within 5 minutes (checkpoint failure rate interval), if
              the number of checkpoint failures reaches 10 (max failures per interval),action will be triggered(alert or restart job)
            </span>
          </p>
        </div>
      );
    };
  },
});
