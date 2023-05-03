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
<template>
  <BasicDrawer
    :okText="t('common.submitText')"
    @register="registerDrawer"
    showFooter
    width="650"
    @ok="handleSubmit"
  >
    <template #title>
      <Icon icon="ant-design:code-outlined" />
      {{ getTitle }}
    </template>
    <BasicForm @register="registerForm" :schemas="getResourceFormSchema">
      <template #uploadJobJar>
        <UploadJobJar :custom-request="handleCustomJobRequest" v-model:loading="uploadLoading">
          <template #uploadInfo>
            <Alert v-if="uploadJar" class="uploadjar-box" type="info">
              <template #message>
                <span class="tag-dependency-pom">
                  {{ uploadJar }}
                </span>
              </template>
            </Alert>
          </template>
        </UploadJobJar>
      </template>
    </BasicForm>
  </BasicDrawer>
</template>
<script lang="ts">
  export default {
    name: 'ResourceDrawer',
  };
</script>

<script lang="ts" setup>
  import { ref, h, computed, unref } from 'vue';
  import { Alert } from 'ant-design-vue';
  import { BasicForm, FormSchema, useForm } from '/@/components/Form';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import { Icon } from '/@/components/Icon';
  import { useI18n } from '/@/hooks/web/useI18n';
  import UploadJobJar from '/@/views/flink/app/components/UploadJobJar.vue';
  import { fetchUpload } from "/@/api/flink/app/app";
  import { fetchAddResource, fetchUpdateResource } from "/@/api/flink/resource";
  import { ResourceTypeEnum } from "/@/views/flink/resource/resource.data";

  const emit = defineEmits(['success', 'register']);

  const { t } = useI18n();

  const isUpdate = ref(false);
  const uploadLoading = ref(false);
  const uploadJar = ref('');
  const resourceId = ref<Nullable<number>>(null);

  const getResourceFormSchema = computed((): FormSchema[] => {
    return [
      {
        field: 'resourceType',
        label: t('flink.resource.resourceType'),
        component: 'Select',
        componentProps: {
          options: [
            { label: 'APP', value: ResourceTypeEnum.APP },
            { label: 'COMMON', value: ResourceTypeEnum.COMMON },
            { label: 'CONNECTOR', value: ResourceTypeEnum.CONNECTOR },
            { label: 'FORMAT', value: ResourceTypeEnum.FORMAT },
            { label: 'UDF', value: ResourceTypeEnum.UDF },
          ],
        },
      },
      {
        field: 'resourceName',
        label: t('flink.resource.uploadResource'),
        component: 'Select',
        slot: 'uploadJobJar',
      },
      {
        field: 'mainClass',
        label: t('flink.app.mainClass'),
        component: 'Input',
        componentProps: { placeholder: t('flink.app.addAppTips.mainClassPlaceholder') },
        ifShow: ({ values }) => values?.resourceType == 'APP',
        rules: [{ required: true, message: t('flink.app.addAppTips.mainClassIsRequiredMessage') }],
      },
      {
        field: 'description',
        label: t('common.description'),
        component: 'InputTextArea',
        componentProps: { rows: 4 },
        rules: [{ max: 100, message: t('flink.resource.form.descriptionMessage') }],
      },
    ];
  });

  const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
    name: 'ResourceForm',
    colon: true,
    showActionButtonGroup: false,
    baseColProps: { span: 24 },
    labelCol: { lg: { span: 5, offset: 0 }, sm: { span: 7, offset: 0 } },
    wrapperCol: { lg: { span: 16, offset: 0 }, sm: { span: 17, offset: 0 } },
  });

  const [registerDrawer, { setDrawerProps, changeLoading, closeDrawer }] = useDrawerInner(
    async (data: Recordable) => {
      resetFields();
      setDrawerProps({ confirmLoading: false });
      isUpdate.value = !!data?.isUpdate;
      if (unref(isUpdate)) {
        resourceId.value = data.record.id;
        setFieldsValue(data.record);
      }
    },
  );

  const getTitle = computed(() =>
    !unref(isUpdate) ? t('flink.resource.addResource') : t('flink.resource.modifyResource'),
  );

  // form submit
  async function handleSubmit() {
    try {
      const values = await validate();
      setDrawerProps({ confirmLoading: true });
      await (isUpdate.value
        ? fetchUpdateResource({ id: resourceId.value, ...values })
        : fetchAddResource(values));
      uploadJar.value = ''
      closeDrawer();
      emit('success', isUpdate.value);
    } finally {
      setDrawerProps({ confirmLoading: false });
    }
  }

  /* Custom job upload */
  async function handleCustomJobRequest(data) {
    const formData = new FormData();
    formData.append('file', data.file);
    try {
      const path = await fetchUpload(formData);
      uploadJar.value = data.file.name;
      uploadLoading.value = false;
      setFieldsValue({ resourceName: uploadJar.value });
    } catch (error) {
      console.error(error);
      uploadLoading.value = false;
    }
  }
</script>

<style lang="less">
  .conf-switch {
    display: inline-block;
    margin-top: 10px;
    color: darkgrey;
  }
</style>
