<script lang="ts">
  export default defineComponent({
    name: 'AddProject',
  });
</script>

<script setup lang="ts" name="AddProject">
  import { defineComponent } from 'vue';
  import { useProject } from './useProject';
  import { createProject } from '/@/api/flink/project';
  import { BasicForm } from '/@/components/Form';
  import { PageWrapper } from '/@/components/Page';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { useMessage } from '/@/hooks/web/useMessage';
  const { getLoading, close, registerForm, submit, handleSubmit } = useProject();

  const { createMessage } = useMessage();
  const { t } = useI18n();

  /* 创建项目 */
  async function handleCreateAction(values: Recordable) {
    try {
      const res = await createProject({
        name: values.name,
        url: values.url,
        repository: values.repository,
        type: values.type,
        branches: values.branches,
        userName: values.userName,
        password: values.password,
        pom: values.pom,
        buildArgs: values.buildArgs,
        description: values.description,
      });
      if (res.data) {
        createMessage.success('created successfully');
        close(undefined, { path: '/flink/project' });
      } else {
        createMessage.error('Project save failed :' + res['message']);
      }
    } catch (error: any) {
      if (error?.data?.message) {
        createMessage.error('Project save failed:' + error.data['message']);
      } else {
        createMessage.error('Project save failed ..>﹏<.. ');
      }
    }
  }
</script>
<template>
  <PageWrapper contentFullHeight contentBackground contentClass="p-26px">
    <BasicForm
      @register="registerForm"
      @submit="(values:Recordable)=>handleSubmit(values,handleCreateAction)"
    >
      <template #formFooter>
        <div class="flex items-center w-full justify-center">
          <a-button @click="close(undefined, { path: '/flink/app' })">
            {{ t('common.cancelText') }}
          </a-button>
          <a-button class="ml-4" :loading="getLoading" type="primary" @click="submit()">
            {{ t('common.submitText') }}
          </a-button>
        </div>
      </template>
    </BasicForm>
  </PageWrapper>
</template>
