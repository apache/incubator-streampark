<!--
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
-->
<script lang="ts">
  import { defineComponent } from 'vue';
  import { omit } from 'lodash-es';
  import { alertFormSchema } from './alert.data';

  export default defineComponent({
    name: 'AlertModal',
  });
</script>
<script setup lang="ts" name="AlertModal">
  import { ref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form';
  import { Form, Select, Input, Divider, Tooltip, Switch } from 'ant-design-vue';
  import { SvgIcon } from '/@/components/Icon';
  import { fetchAlertAdd, fetchAlertUpdate, fetchExistsAlert } from '/@/api/flink/setting/alert';
  import { useUserStoreWithOut } from '/@/store/modules/user';
  import { useMessage } from '/@/hooks/web/useMessage';

  const FormItem = Form.Item;
  const SelectOption = Select.Option;
  const InputTextArea = Input.TextArea;

  const emit = defineEmits(['reload', 'register']);
  const alertId = ref<string | null>(null);
  const alertTypes = ref([
    { name: 'E-mail', value: 1, disabled: false, icon: 'mail' },
    { name: 'Ding Talk', value: 2, disabled: false, icon: 'dingtalk' },
    { name: 'Wechat', value: 4, disabled: false, icon: 'wecom' },
    { name: 'SMS', value: 8, disabled: true, icon: 'message' },
    { name: 'Lark', value: 16, disabled: false, icon: 'lark' },
  ]);
  const alertType = ref<number[]>([]);
  const dingtalkSecretEnable = ref(false);
  const larkSecretEnable = ref(false);

  const { createConfirm, createMessage } = useMessage();
  const userStore = useUserStoreWithOut();
  const [registerModal, { changeOkLoading, closeModal }] = useModalInner((data) => {
    if (data) {
      alertId.value = data.alertId;
      alertType.value = data.alertType;
      setFieldsValue(omit(data, 'alertId'));
    }
  });
  const [registerForm, { validateFields, setFieldsValue }] = useForm({
    labelWidth: 160,
    colon: true,
    showActionButtonGroup: false,
    baseColProps: { span: 24 },
    labelCol: { lg: 5, sm: 7 },
    wrapperCol: { lg: 16, sm: 4 },
    schemas: [
      {
        field: 'alertName',
        label: 'Alert Name',
        component: 'Input',
        componentProps: { allowClear: true, placeholder: 'Please enter alert name' },
        helpMessage: 'the alert name, e.g: streamx team alert',
        colProps: {
          style: { marginBottom: '20px' },
        },
        dynamicRules: () => {
          return [
            {
              validator: async (_, value) => {
                if (value === null || value === undefined || value === '') {
                  return Promise.reject('Alert Name is required');
                } else {
                  if (!alertId.value) {
                    try {
                      const isExist = await fetchExistsAlert({
                        alertName: value,
                        isJsonType: true,
                      });
                      if (isExist) {
                        return Promise.reject(
                          'Alert Name must be unique. The alert name already exists',
                        );
                      } else {
                        return Promise.resolve();
                      }
                    } catch (error) {
                      return Promise.reject('error happened ,caused by: ' + error);
                    }
                  }
                }
                return Promise.resolve();
              },
              required: true,
              message: 'Alert Name is required',
            },
          ];
        },
      },
      ...alertFormSchema,
    ],
  });

  // Submit new settings
  async function handleSubmit() {
    try {
      changeOkLoading(true);
      const formValue = await validateFields();
      const param = {
        id: alertId.value,
        alertName: formValue.alertName,
        userId: userStore.getUserInfo?.userId,
        alertType: eval(formValue.alertType.join('+')),
        emailParams: { contacts: formValue.alertEmail },
        dingTalkParams: {
          token: formValue.dingtalkToken,
          contacts: formValue.alertDingUser,
          isAtAll: formValue.dingtalkIsAtAll,
          alertDingURL: formValue.alertDingURL,
          secretEnable: formValue.dingtalkSecretEnable,
          secretToken: formValue.dingtalkSecretToken,
        },
        weComParams: {
          token: formValue.weToken,
        },
        larkParams: {
          token: formValue.larkToken,
          isAtAll: formValue.larkIsAtAll,
          secretEnable: formValue.larkSecretEnable,
          secretToken: formValue.larkSecretToken,
        },
        isJsonType: true,
      };
      console.log('Update alarm parameters：' + JSON.stringify(param));

      // No id means new operation
      if (!param.id) {
        // Check if there is an alarm with the same name before submitting
        const isExist = await fetchExistsAlert({ alertName: param.alertName });
        if (isExist) {
          createConfirm({
            iconType: 'error',
            title: 'Failed create AlertConfig',
            content: `alertName ${param.alertName} is already exists!`,
          });
        } else {
          await fetchAlertAdd(param);
          createMessage.success('Create AlertConfig successful!');
        }
      } else {
        //更新操作
        await fetchAlertUpdate(param);
        createMessage.success('Update AlertConfig successful!');
      }
      closeModal();
      emit('reload');
    } catch (error) {
      console.error(error);
    } finally {
      changeOkLoading(false);
    }
  }
</script>

<template>
  <BasicModal @register="registerModal" v-bind="$attrs" @ok="handleSubmit">
    <template #title>
      <SvgIcon name="alarm" size="25" />
      Alert Setting
    </template>
    <BasicForm @register="registerForm">
      <template #type="{ model, field }">
        <Select
          v-model:value="model[field]"
          placeholder="Alert Type"
          allowClear
          mode="multiple"
          @change="(value:number[])=>alertType=value"
        >
          <SelectOption
            v-for="(o, index) in alertTypes"
            :key="`alertType_${index}`"
            :disabled="o.disabled"
            :value="o.value"
          >
            <SvgIcon :name="o.icon" />
            {{ o.name }}
          </SelectOption>
        </Select>
      </template>

      <template #alertEmail="{ model, field }">
        <!-- Alert Email -->
        <Divider v-if="alertType.indexOf(1) > -1">
          <SvgIcon name="mail" size="20" />
          E-mail
        </Divider>
        <FormItem
          v-if="alertType.indexOf(1) > -1"
          label="Alert Email"
          :rules="[{ required: true, message: 'email address is required', trigger: 'blur' }]"
          name="alertEmail"
        >
          <Input
            v-model:value="model[field]"
            placeholder="Please enter email,separate multiple emails with comma(,)"
          />
        </FormItem>
      </template>

      <template #alertDingURL="{ model, field }" v-if="alertType.indexOf(2) > -1">
        <Divider v-if="alertType.indexOf(2) > -1">
          <SvgIcon name="dingtalk" size="20" />
          Ding Talk
        </Divider>
        <FormItem
          label="DingTalk Url"
          defaultValue="https://oapi.dingtalk.com/robot/send"
          name="alertEmail"
        >
          <Input v-model:value="model[field]" placeholder="Please enter DingTask Url" allowClear />
        </FormItem>
      </template>

      <template #dingtalkToken="{ model, field }" v-if="alertType.indexOf(2) > -1">
        <FormItem
          label="Access Token"
          name="dingtalkToken"
          :rules="[{ required: true, message: 'Access token is required' }]"
        >
          <Input
            v-model:value="model[field]"
            placeholder="Please enter the access token of DingTalk"
            allowClear
          />
        </FormItem>
      </template>

      <template #dingtalkSecretEnable="{ model, field }" v-if="alertType.indexOf(2) > -1">
        <FormItem label="Secret Enable" name="dingtalkSecretEnable">
          <Tooltip title="DingTalk ecretToken is enable">
            <Switch
              v-model:checked="model[field]"
              checked-children="ON"
              un-checked-children="OFF"
              allowClear
              @change="(checked:boolean) => (dingtalkSecretEnable = checked)"
            />
          </Tooltip>
        </FormItem>
      </template>
      <!-- Secret Token -->
      <template
        #dingtalkSecretToken="{ model, field }"
        v-if="alertType.indexOf(2) > -1 && dingtalkSecretEnable"
      >
        <FormItem
          label="Secret Token"
          name="dingtalkSecretToken"
          :rules="[{ required: true, message: 'DingTalk SecretToken is required' }]"
        >
          <Input
            v-model:value="model[field]"
            placeholder="Please enter DingTalk SecretToken"
            allowClear
          />
        </FormItem>
      </template>

      <!-- DingTalk User -->
      <template #alertDingUser="{ model, field }" v-if="alertType.indexOf(2) > -1">
        <FormItem label="DingTalk User" name="alertDingUser">
          <Input
            v-model:value="model[field]"
            placeholder="Please enter DingTalk receive user"
            allowClear
          />
        </FormItem>
      </template>

      <!-- At All User -->
      <template #dingtalkIsAtAll="{ model, field }" v-if="alertType.indexOf(2) > -1">
        <FormItem label="At All User">
          <Tooltip title="Whether Notify All">
            <Switch
              v-model:checked="model[field]"
              checked-children="ON"
              un-checked-children="OFF"
            />
          </Tooltip>
        </FormItem>
      </template>

      <!-- WeChat -->
      <template #weToken="{ model, field, schema }" v-if="alertType.indexOf(4) > -1">
        <Divider><SvgIcon name="wecom" size="20" /> WeChat </Divider>
        <FormItem :label="schema.label" :name="field" :rules="schema.rules">
          <Input v-model:value="model[field]" v-bind="schema.componentProps" />
        </FormItem>
      </template>

      <template #alertSms="{ model, field, schema }" v-if="alertType.indexOf(8) > -1">
        <Divider><SvgIcon name="message" size="20" /> SMS </Divider>
        <FormItem :label="schema.label" :name="field" :rules="schema.rules">
          <Input v-model:value="model[field]" v-bind="schema.componentProps" />
        </FormItem>
      </template>

      <template #alertSmsTemplate="{ model, field, schema }" v-if="alertType.indexOf(8) > -1">
        <FormItem :label="schema.label" :name="field" :rules="schema.rules">
          <InputTextArea v-model:value="model[field]" v-bind="schema.componentProps" />
        </FormItem>
      </template>

      <template #larkToken="{ model, field, schema }" v-if="alertType.indexOf(16) > -1">
        <Divider><SvgIcon name="lark" size="20" /> Lark </Divider>
        <FormItem :label="schema.label" :name="field" :rules="schema.rules">
          <Input v-model:value="model[field]" v-bind="schema.componentProps" />
        </FormItem>
      </template>

      <template #larkIsAtAll="{ model, field, schema }" v-if="alertType.indexOf(16) > -1">
        <FormItem :label="schema.label" :name="field" :rules="schema.rules">
          <Tooltip title="Whether Notify All">
            <Switch
              checked-children="ON"
              un-checked-children="OFF"
              allowClear
              v-model:checked="model[field]"
              @change="(checked:boolean) => (larkSecretEnable = checked)"
            />
          </Tooltip>
        </FormItem>
      </template>

      <template
        #larkSecretToken="{ model, field, schema }"
        v-if="alertType.indexOf(16) > -1 && larkSecretEnable"
      >
        <FormItem :label="schema.label" :name="field" :rules="schema.rules">
          <Input v-bind="schema.componentProps" v-model:checked="model[field]" />
        </FormItem>
      </template>
    </BasicForm>
  </BasicModal>
</template>
