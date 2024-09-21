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
  export default defineComponent({
    name: 'AlertModal',
  });
</script>
<script setup lang="ts" name="AlertModal">
  import { ref, defineComponent, h } from 'vue';
  import { omit } from 'lodash-es';
  import { alertFormSchema, alertTypes } from './index.data';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form';
  import { Form, Select, Input, Divider } from 'ant-design-vue';
  import { SvgIcon } from '/@/components/Icon';
  import { fetchAlertAdd, fetchAlertUpdate, fetchExistsAlert } from '/@/api/setting/alert';
  import { useUserStore } from '/@/store/modules/user';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useI18n } from '/@/hooks/web/useI18n';

  const FormItem = Form.Item;
  const SelectOption = Select.Option;
  const InputTextArea = Input.TextArea;
  const { t } = useI18n();
  const emit = defineEmits(['reload', 'register']);
  const alertId = ref<string | null>(null);
  const alertType = ref<string[]>([]);

  const { Swal } = useMessage();
  const userStore = useUserStore();
  const [registerForm, { validateFields, resetFields, setFieldsValue }] = useForm({
    colon: true,
    showActionButtonGroup: false,
    layout: 'vertical',
    baseColProps: { span: 22, offset: 1 },
    schemas: [
      {
        field: 'alertName',
        label: t('setting.alarm.alertName'),
        component: 'Input',
        componentProps: {
          allowClear: true,
          placeholder: t('setting.alarm.alertNamePlaceHolder'),
        },
        afterItem: () => h('span', { class: 'pop-tip' }, t('setting.alarm.alertNameTips')),
        dynamicRules: () => {
          return [
            {
              validator: async (_, value) => {
                if (value === null || value === undefined || value === '') {
                  return Promise.reject(
                    t('setting.alarm.alertNameErrorMessage.alertNameIsRequired'),
                  );
                } else {
                  if (!alertId.value) {
                    try {
                      const isExist = await fetchExistsAlert({ alertName: value });
                      if (isExist) {
                        return Promise.reject(
                          t('setting.alarm.alertNameErrorMessage.alertNameAlreadyExists'),
                        );
                      } else {
                        return Promise.resolve();
                      }
                    } catch (error) {
                      return Promise.reject(
                        t('setting.alarm.alertNameErrorMessage.alertConfigFailed') + error,
                      );
                    }
                  }
                }
                return Promise.resolve();
              },
              required: true,
              trigger: 'blur',
            },
          ];
        },
      },
      ...alertFormSchema,
    ],
  });
  const [registerModal, { changeOkLoading, closeModal }] = useModalInner((data) => {
    resetFields();
    alertId.value = '';
    alertType.value = [];
    if (data && Object.keys(data).length > 0) {
      alertId.value = data.alertId;
      alertType.value = data.alertType;
      setFieldsValue(omit(data, 'alertId'));
    }
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
        weComParams: { token: formValue.weToken },
        larkParams: {
          token: formValue.larkToken,
          isAtAll: formValue.larkIsAtAll,
          secretEnable: formValue.larkSecretEnable,
          secretToken: formValue.larkSecretToken,
        },
        isJsonType: true,
      };

      // No id means new operation
      if (!param.id) {
        // Check if there is an alarm with the same name before submitting
        const isExist = await fetchExistsAlert({ alertName: param.alertName });
        if (isExist) {
          Swal.fire(
            t('setting.alarm.fail.title'),
            t('setting.alarm.fail.subTitle', [param.alertName]),
            'error',
          );
        } else {
          const { data } = await fetchAlertAdd(param);
          if (!data.data) {
            Swal.fire(
              t('setting.alarm.fail.title'),
              data['message'].replaceAll(/\[StreamPark]/g, ''),
              'error',
            );
          } else {
            Swal.fire({
              icon: 'success',
              title: t('setting.alarm.success.title'),
              showConfirmButton: false,
              timer: 2000,
            });
          }
        }
      } else {
        //update
        const { data } = await fetchAlertUpdate(param);
        if (!data.data) {
          Swal.fire(
            t('setting.alarm.fail.update'),
            data['message'].replaceAll(/\[StreamPark]/g, ''),
            'error',
          );
        } else {
          Swal.fire({
            icon: 'success',
            title: t('setting.alarm.success.update'),
            showConfirmButton: false,
            timer: 2000,
          });
        }
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
  <BasicModal
    :okButtonProps="{ class: 'e2e-alert-submit-btn' }"
    :cancelButtonProps="{ class: 'e2e-alert-cancel-btn' }"
    :width="650"
    :ok-text="t('common.submitText')"
    @register="registerModal"
    v-bind="$attrs"
    @ok="handleSubmit"
  >
    <template #title>
      <SvgIcon name="alarm" size="25" />
      {{ t('setting.alarm.alertSetting') }}
    </template>
    <div class="mt-3">
      <BasicForm @register="registerForm" class="!mt-15px">
        <template #type="{ model, field }">
          <Select
            v-model:value="model[field]"
            :placeholder="t('setting.alarm.faultAlertType')"
            allowClear
            mode="multiple"
            @change="(value: string[]) => (alertType = value)"
          >
            <SelectOption
              v-for="(v, k) in alertTypes"
              :key="`alertType_${k}`"
              :disabled="v.disabled"
              :value="k"
            >
              <SvgIcon :name="v.icon" />
              {{ v.name }}
            </SelectOption>
          </Select>
        </template>
        <template #alertEmail="{ model, field }">
          <!-- Alert Email -->
          <template v-if="(alertType || []).includes('1')">
            <Divider>
              <SvgIcon name="mail" size="20" />
              {{ t('setting.alarm.email') }}
            </Divider>
            <FormItem
              :label="t('setting.alarm.alertEmail')"
              :rules="[
                {
                  required: true,
                  message: t('setting.alarm.alertEmailAddressIsRequired'),
                  trigger: 'blur',
                },
                {
                  pattern:
                    /^([a-zA-Z0-9_.-]+@[a-zA-Z0-9-]+(\.[a-zA-Z0-9-]+)*\.[a-zA-Z0-9]{2,6})(,[a-zA-Z0-9_.-]+@[a-zA-Z0-9-]+(\.[a-zA-Z0-9-]+)*\.[a-zA-Z0-9]{2,6})*?$/,
                  message: t('setting.alarm.alertEmailFormatIsInvalid'),
                  trigger: 'blur',
                },
              ]"
              name="alertEmail"
            >
              <Input
                v-model:value="model[field]"
                :placeholder="t('setting.alarm.alertEmailPlaceholder')"
              />
            </FormItem>
          </template>
        </template>

        <template #alertDingURL="{ model, field }" v-if="(alertType || []).includes('2')">
          <Divider>
            <SvgIcon name="dingtalk" size="20" />
            {{ t('setting.alarm.dingTalk') }}
          </Divider>
          <FormItem
            :label="t('setting.alarm.dingTalkUrl')"
            name="alertDingURL"
            :rules="[
              {
                pattern:
                  /^((https?):\/\/)?([^!@#$%^&*?.\s-]([^!@#$%^&*?.\s]{0,63}[^!@#$%^&*?.\s])?\.)+[a-z]{2,6}\/?/,
                message: t('setting.alarm.dingTalkUrlFormatIsInvalid'),
                trigger: 'blur',
              },
            ]"
          >
            <Input
              v-model:value="model[field]"
              :placeholder="t('setting.alarm.dingTalkPlaceholder')"
              allowClear
            />
          </FormItem>
        </template>

        <!-- WeChat -->
        <template #weToken="{ model, field, schema }" v-if="(alertType || []).includes('4')">
          <Divider> <SvgIcon name="wecom" size="20" /> {{ t('setting.alarm.weChat') }} </Divider>
          <FormItem :label="schema.label" :name="field" :rules="schema.rules">
            <InputTextArea v-model:value="model[field]" v-bind="schema.componentProps" />
          </FormItem>
        </template>

        <template #alertSms="{ model, field, schema }" v-if="(alertType || []).includes('8')">
          <Divider> <SvgIcon name="message" size="20" /> {{ t('setting.alarm.sms') }} </Divider>
          <FormItem :label="schema.label" :name="field" :rules="schema.rules">
            <Input v-model:value="model[field]" v-bind="schema.componentProps" />
          </FormItem>
        </template>

        <!-- lark -->
        <template #larkToken="{ model, field, schema }" v-if="(alertType || []).includes('16')">
          <Divider> <SvgIcon name="lark" size="20" /> {{ t('setting.alarm.lark') }} </Divider>
          <FormItem :label="schema.label" :name="field" :rules="schema.rules">
            <Input
              v-model:value="model[field]"
              :placeholder="t('setting.alarm.larkTokenPlaceholder')"
              allow-clear
            />
          </FormItem>
        </template>
      </BasicForm>
    </div>
  </BasicModal>
</template>
