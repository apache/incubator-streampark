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
  import { defineComponent, reactive } from 'vue';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { exceptionPropWidth } from '/@/utils';

  export default defineComponent({
    name: 'StartApplicationModal',
  });
</script>
<script setup lang="ts" name="StartApplicationModal">
  import { h, ref } from 'vue';
  import { Select, Input, Tag } from 'ant-design-vue';
  import { BasicForm, useForm } from '/@/components/Form';
  import { SvgIcon, Icon } from '/@/components/Icon';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useRouter } from 'vue-router';
  import { fetchCheckStart, fetchStart, fetchAbort } from '/@/api/flink/app';
  import { AppExistsEnum } from '/@/enums/flinkEnum';

  const SelectOption = Select.Option;

  const { t } = useI18n();
  const { Swal } = useMessage();
  const router = useRouter();
  const selectInput = ref<boolean>(false);
  const selectValue = ref<string>(null);

  const emits = defineEmits(['register', 'updateOption']);
  const receiveData = reactive<Recordable>({});

  const [registerModal, { closeModal }] = useModalInner((data) => {
    if (data) {
      Object.assign(receiveData, data);
      resetFields();
      setFieldsValue({
        restoreSavepoint: receiveData.selected != null,
        savepointPath: receiveData.selected?.path,
      });
    }
  });

  function handleSavePointTip(list) {
    if (list != null && list.length > 0) {
      return t('flink.app.view.savepointSwitch');
    }
    return t('flink.app.view.savepointInput');
  }

  const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
    name: 'startApplicationModal',
    layout: 'vertical',
    baseColProps: { span: 22, offset: 1 },
    schemas: [
      {
        field: 'restoreSavepoint',
        label: t('flink.app.view.fromSavepoint'),
        component: 'Switch',
        componentProps: {
          checkedChildren: 'ON',
          unCheckedChildren: 'OFF',
        },
        defaultValue: false,
        afterItem: () => h('span', { class: 'pop-tip' }, t('flink.app.view.savepointTip')),
      },
      {
        field: 'savepointPath',
        label: 'Savepoint',
        component:
          receiveData.historySavePoint && receiveData.historySavePoint.length > 0
            ? 'Select'
            : 'Input',
        afterItem: () =>
          h('span', { class: 'pop-tip' }, handleSavePointTip(receiveData.historySavePoint)),
        slot: 'savepoint',
        ifShow: ({ values }) => values.restoreSavepoint,
        required: true,
      },
      {
        field: 'allowNonRestoredState',
        label: t('flink.app.view.ignoreRestored'),
        component: 'Switch',
        componentProps: {
          checkedChildren: 'ON',
          unCheckedChildren: 'OFF',
        },
        afterItem: () => h('span', { class: 'pop-tip' }, t('flink.app.view.ignoreRestoredTip')),
        defaultValue: false,
        ifShow: ({ values }) => values.restoreSavepoint,
      },
    ],
    colon: true,
    showActionButtonGroup: false,
  });

  async function handleSubmit() {
    // when then app is building, show forced starting modal
    const resp = await fetchCheckStart({
      id: receiveData.application.id,
    });
    if (resp.data.data === AppExistsEnum.IN_YARN) {
      await fetchAbort({
        id: receiveData.application.id,
      });
    }
    await handleDoSubmit();
  }

  async function handleReset() {
    selectInput.value = false;
    selectValue.value = null;
  }

  /* submit */
  async function handleDoSubmit() {
    try {
      const formValue = (await validate()) as Recordable;
      const restoreOrTriggerSavepoint = formValue.restoreSavepoint;
      const savepointPath = restoreOrTriggerSavepoint ? formValue['savepointPath'] : null;
      await handleReset();
      const { data } = await fetchStart({
        id: receiveData.application.id,
        restoreOrTriggerSavepoint,
        savepointPath: savepointPath,
        allowNonRestored: formValue.allowNonRestoredState || false,
      });
      if (data.data) {
        Swal.fire({
          icon: 'success',
          title: t('flink.app.operation.starting'),
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

  function handleSavepoint(model, field, input) {
    selectInput.value = input;
    if (input) {
      selectValue.value = model[field];
      model[field] = null;
    } else {
      model[field] = selectValue.value;
    }
  }
</script>
<template>
  <BasicModal
    :okButtonProps="{ id: 'e2e-flinkapp-start-submit' }"
    :cancelButtonProps="{ id: 'e2e-flinkapp-start-cancel' }"
    @register="registerModal"
    :minHeight="100"
    @ok="handleSubmit"
    @cancel="handleReset"
    :okText="t('common.apply')"
    :cancelText="t('common.cancelText')"
  >
    <template #title>
      <SvgIcon name="play" />
      {{ t('flink.app.view.start') }}
    </template>

    <BasicForm @register="registerForm" class="!pt-20px">
      <template #savepoint="{ model, field }">
        <template
          v-if="
            !selectInput && receiveData.historySavePoint && receiveData.historySavePoint.length > 0
          "
        >
          <Select v-model:value="model[field]" @dblclick="handleSavepoint(model, field, true)">
            <SelectOption v-for="(k, i) in receiveData.historySavePoint" :key="i" :value="k.path">
              <span style="color: darkgrey">
                <Icon icon="ant-design:clock-circle-outlined" />
                {{ k.createTime }}
              </span>
              <span style="float: left" v-if="k.type === 1">
                <tag color="cyan">CP</tag>
              </span>
              <span style="float: right" v-else>
                <tag color="blue">SP</tag>
              </span>
              <span style="float: right" v-if="k.latest">
                <tag color="#2db7f5">latest</tag>
              </span>
            </SelectOption>
          </Select>
        </template>
        <Input
          v-else
          @dblclick="handleSavepoint(model, field, false)"
          type="text"
          :placeholder="$t('flink.app.view.savepointInput')"
          v-model:value="model[field]"
        />
      </template>
    </BasicForm>
  </BasicModal>
</template>
