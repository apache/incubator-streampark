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
  import { ref, defineComponent } from 'vue';
  import { useRouter } from 'vue-router';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form';
//   import { Form, Select, Input, Divider } from 'ant-design-vue';
  import { SvgIcon } from '/@/components/Icon';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { useCreateSchema } from './hooks/useCreateSchema';
  // import PomTemplateTab from './components/PodTemplate/PomTemplateTab.vue';
  // import { createAsyncComponent } from '/@/utils/factory/createAsyncComponent';
  // import {
  //   JobTypeEnum
  // } from '/@/enums/flinkEnum';

//   const FormItem = Form.Item;
//   const SelectOption = Select.Option;
//   const InputTextArea = Input.TextArea;
  const { t } = useI18n();
  const emit = defineEmits(['reload', 'register']);
  const alertId = ref<string | null>(null);
  const alertType = ref<string[]>([]);
  const dependencyRef = ref();
  const router = useRouter();
  // const k8sTemplate = reactive({
  //   podTemplate: '',
  //   jmPodTemplate: '',
  //   tmPodTemplate: '',
  // });

  // const [registerAppForm, { setFieldsValue, getFieldsValue, submit }] = useForm({
  //   labelCol: { lg: { span: 5, offset: 0 }, sm: { span: 7, offset: 0 } },
  //   wrapperCol: { lg: { span: 16, offset: 0 }, sm: { span: 17, offset: 0 } },
  //   baseColProps: { span: 24 },
  //   colon: true,
  //   showActionButtonGroup: false,
  // });
  //flinkEnvs, flinkClusters, 
  const { getFirstCreateFormSchema } = useCreateSchema(dependencyRef);
  const [registerAppForm, { validateFields, resetFields }] = useForm({
    labelWidth: 160,
    colon: true,
    showActionButtonGroup: false,
    baseColProps: { span: 24 },
    labelCol: { lg: 5, sm: 7 },
    wrapperCol: { lg: 16, sm: 4 }
  });
  const [registerModal, { changeOkLoading, closeModal }] = useModalInner((data) => {
    resetFields();
    alertId.value = '';
    alertType.value = [];
    if (data && Object.keys(data).length > 0) {
      alertId.value = data.alertId;
      alertType.value = data.alertType;
      // setFieldsValue(omit(data, 'alertId'));
    }
  });
  // const Dependency = createAsyncComponent(() => import('./components/Dependency.vue'), {
  //   loading: true,
  // });
  // Submit new settings
  async function handleSubmit() {
    try {
      changeOkLoading(true);
      const formValue = await validateFields();
      console.log(JSON.stringify(formValue));
      // Object.assign(formValue, {jobType: JobTypeEnum.SQL})
      sessionStorage.removeItem('AddJobModalParams');
      sessionStorage.setItem('AddJobModalParams', JSON.stringify(formValue));
      router.push({ path: '/flink/app/add' });
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
    class="app_controller"
    :ok-text="t('common.submitText')"
    @register="registerModal"
    v-bind="$attrs"
    @ok="handleSubmit"
  >
    <template #title>
      <SvgIcon name="plus" size="20" />
      {{ t('flink.app.alertAdd') }}
    </template>
    <BasicForm @register="registerAppForm" class="!mt-15px" :schemas="getFirstCreateFormSchema">
      <!-- <template #dependency="{ model, field }">
        <Dependency
          ref="dependencyRef"
          v-model:value="model[field]"
          :form-model="model"
          :flink-envs="flinkEnvs"
        />
      </template> -->
      <!-- <template #podTemplate>
        <PomTemplateTab
          v-model:podTemplate="k8sTemplate.podTemplate"
          v-model:jmPodTemplate="k8sTemplate.jmPodTemplate"
          v-model:tmPodTemplate="k8sTemplate.tmPodTemplate"
        />
      </template> -->
    </BasicForm>
  </BasicModal>
</template>
<style lang="less">
  @import url('./styles/Add.less');
</style>
