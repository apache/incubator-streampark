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
<script lang="ts" setup>
  import { computed, nextTick, onMounted, ref } from 'vue';
  import { useGo } from '/@/hooks/web/usePage';
  import ProgramArgs from './ProgramArgs.vue';
  import { createAsyncComponent } from '/@/utils/factory/createAsyncComponent';

  import { BasicForm, useForm } from '/@/components/Form';
  import { useDrawer } from '/@/components/Drawer';
  import { useI18n } from '/@/hooks/web/useI18n';
  import VariableReview from './VariableReview.vue';
  import { useSparkSchema } from '../hooks/useAppFormSchema';
  import { SparkEnv } from '/@/api/spark/home.type';
  import { decodeByBase64 } from '/@/utils/cipher';

  const SparkSqlEditor = createAsyncComponent(() => import('./SparkSql.vue'), {
    loading: true,
  });

  defineOptions({ name: 'AppForm' });
  const props = defineProps({
    sparkEnvs: {
      type: Array as PropType<SparkEnv[]>,
      default: () => [],
    },
    initFormFn: {
      type: Function as PropType<() => Promise<Recordable>>,
    },
    submit: {
      type: Function as PropType<(params: Recordable) => Promise<void>>,
      required: true,
    },
  });
  const go = useGo();
  const sparkSql = ref();
  const programArgsRef = ref<{
    setContent: (data: string) => void;
  }>();
  const submitLoading = ref(false);

  const { t } = useI18n();

  const getSparkEnvs = computed<SparkEnv[]>(() => props.sparkEnvs);
  const { formSchema, suggestions } = useSparkSchema(getSparkEnvs);

  const [registerAppForm, { setFieldsValue, submit: submitForm }] = useForm({
    labelCol: { lg: { span: 5, offset: 0 }, sm: { span: 7, offset: 0 } },
    wrapperCol: { lg: { span: 16, offset: 0 }, sm: { span: 17, offset: 0 } },
    baseColProps: { span: 24 },
    colon: true,
    showActionButtonGroup: false,
  });

  defineExpose({
    sparkSql,
  });
  const [registerReviewDrawer, { openDrawer: openReviewDrawer }] = useDrawer();

  /* Initialize the form */
  async function handleInitForm() {
    if (props.initFormFn) {
      const fieldsValue = await props.initFormFn();
      await setFieldsValue(fieldsValue);
      nextTick(() => {
        if (fieldsValue.sparkSql) sparkSql.value?.setContent(decodeByBase64(fieldsValue.sparkSql));
        if (fieldsValue.appArgs) programArgsRef.value?.setContent(fieldsValue.appArgs);
      });
    }
  }
  /* Submit to create */
  async function handleAppSubmit(formValue: Recordable) {
    try {
      submitLoading.value = true;
      await props.submit(formValue);
    } catch (error) {
      submitLoading.value = false;
    }
  }

  onMounted(async () => {
    handleInitForm();
  });
</script>

<template>
  <BasicForm @register="registerAppForm" @submit="handleAppSubmit" :schemas="formSchema">
    <template #sparkSql="{ model, field }">
      <SparkSqlEditor
        ref="sparkSql"
        v-model:value="model[field]"
        :versionId="model['versionId']"
        :suggestions="suggestions"
        @preview="(value) => openReviewDrawer(true, { value, suggestions })"
      />
    </template>
    <template #args="{ model }">
      <ProgramArgs
        ref="programArgsRef"
        v-model:value="model.args"
        :suggestions="suggestions"
        @preview="(value) => openReviewDrawer(true, { value, suggestions })"
      />
    </template>
    <template #formFooter>
      <div class="flex items-center w-full justify-center">
        <a-button @click="go('/spark/app')">
          {{ t('common.cancelText') }}
        </a-button>
        <a-button class="ml-4" :loading="submitLoading" type="primary" @click="submitForm()">
          {{ t('common.submitText') }}
        </a-button>
      </div>
    </template>
  </BasicForm>
  <VariableReview @register="registerReviewDrawer" />
</template>
