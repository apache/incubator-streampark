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
  import { reactive, defineComponent } from 'vue';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { exceptionPropWidth } from '/@/utils';
  import { isK8sExecMode } from '../../utils';

  export default defineComponent({
    name: 'StartApplicationModal',
  });
</script>
<script setup lang="ts" name="StartApplicationModal">
  import { h } from 'vue';
  import { Select, Input } from 'ant-design-vue';
  import { BasicForm, useForm } from '/@/components/Form';
  import { SvgIcon, Icon } from '/@/components/Icon';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useRouter } from 'vue-router';
  import { fetchStart } from '/@/api/flink/app/app';

  const SelectOption = Select.Option;

  const { t } = useI18n();
  const { Swal } = useMessage();
  const router = useRouter();

  const emits = defineEmits(['register', 'updateOption']);
  const receiveData = reactive<Recordable>({});

  const [registerModal, { closeModal }] = useModalInner((data) => {
    if (data) {
      Object.assign(receiveData, data);
      resetFields();
    }
  });
  const [registerForm, { resetFields, validate }] = useForm({
    name: 'startApplicationModal',
    labelWidth: 120,
    schemas: [
      {
        field: 'flameGraph',
        label: 'flame Graph',
        component: 'Switch',
        componentProps: {
          checkedChildren: 'ON',
          unCheckedChildren: 'OFF',
        },
        defaultValue: false,
        afterItem: () => h('span', { class: 'conf-switch' }, 'flame Graph support'),
        ifShow: () => isK8sExecMode(receiveData.executionMode),
      },
      {
        field: 'startSavePointed',
        label: 'from savepoint',
        component: 'Switch',
        componentProps: {
          checkedChildren: 'ON',
          unCheckedChildren: 'OFF',
        },
        defaultValue: true,
        afterItem: () =>
          h(
            'span',
            { class: 'conf-switch' },
            'restore the application from savepoint or latest checkpoint',
          ),
      },
      {
        field: 'startSavePoint',
        label: 'savepoint',
        component:
          receiveData.historySavePoint && receiveData.historySavePoint.length > 0
            ? 'Select'
            : 'Input',
        afterItem: () =>
          h(
            'span',
            { class: 'conf-switch' },
            'restore the application from savepoint or latest checkpoint',
          ),
        slot: 'savepoint',
        ifShow: ({ values }) => values.startSavePointed && !receiveData.latestSavePoint,
        required: true,
      },
      {
        field: 'allowNonRestoredState',
        label: 'ignore restored',
        component: 'Switch',
        componentProps: {
          checkedChildren: 'ON',
          unCheckedChildren: 'OFF',
        },
        afterItem: () =>
          h('span', { class: 'conf-switch' }, 'ignore savepoint then cannot be restored'),
        defaultValue: false,
        ifShow: ({ values }) => values.startSavePointed,
      },
    ],
    colon: true,
    showActionButtonGroup: false,
    labelCol: { lg: { span: 7, offset: 0 }, sm: { span: 7, offset: 0 } },
    wrapperCol: { lg: { span: 16, offset: 0 }, sm: { span: 4, offset: 0 } },
    baseColProps: { span: 24 },
  });
  /* submit */
  async function handleSubmit() {
    try {
      const formValue = (await validate()) as Recordable;
      const savePointed = formValue.startSavePointed;
      const savePointPath = savePointed
        ? formValue['startSavePoint'] || receiveData.latestSavePoint.savePoint
        : null;
      const { data } = await fetchStart({
        id: receiveData.application.id,
        savePointed,
        savePoint: savePointPath,
        flameGraph: formValue.flameGraph || false,
        allowNonRestored: formValue.allowNonRestoredState || false,
      });
      if (data.data) {
        Swal.fire({
          icon: 'success',
          title: 'The current job is starting',
          showConfirmButton: false,
          timer: 2000,
        });
        emits('updateOption', {
          type: 'starting',
          key: receiveData.application.id,
          value: new Date().getTime(),
        });
        closeModal();
      } else {
        closeModal();
        Swal.fire({
          title: 'Failed',
          icon: 'error',
          width: exceptionPropWidth(),
          html:
            '<pre class="api-exception"> startup failed, ' +
            data.message.replaceAll(/\[StreamPark]/g, '') +
            '</pre>',
          showCancelButton: true,
          confirmButtonColor: '#55BDDDFF',
          confirmButtonText: 'Detail',
          cancelButtonText: 'Close',
        }).then((isConfirm: Recordable) => {
          if (isConfirm.value) {
            router.push({
              path: '/flink/app/detail',
              query: { appId: receiveData.application.id },
            });
          }
        });
      }
    } catch (error) {
      console.error(error);
    }
  }
</script>
<template>
  <BasicModal @register="registerModal" @ok="handleSubmit" okText="Apply" cancelText="Cancel">
    <template #title>
      <SvgIcon name="play" />

      {{ t('flink.app.view.start') }}
    </template>
    <BasicForm @register="registerForm">
      <template #savepoint="{ model, field }">
        <template v-if="receiveData.historySavePoint && receiveData.historySavePoint.length > 0">
          <Select mode="multiple" allow-clear v-model:value="model[field]">
            <SelectOption v-for="(k, i) in receiveData.historySavePoint" :key="i" :value="k.path">
              <span style="color: #108ee9">
                {{ k.path.substr(k.path.lastIndexOf('-') + 1) }}
              </span>
              <span style="float: right; color: darkgrey">
                <Icon icon="ant-design:clock-circle-outlined" />
                {{ k.createTime }}
              </span>
            </SelectOption>
          </Select>
        </template>

        <Input
          v-else
          type="text"
          placeholder="Please enter savepoint manually"
          v-model:value="model[field]"
        />
      </template>
    </BasicForm>
  </BasicModal>
</template>
