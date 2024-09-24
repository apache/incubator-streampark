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
    name: 'AddProject',
  });
</script>

<script setup lang="ts" name="AddProject">
  import { defineComponent } from 'vue';
  import { useRouter } from 'vue-router';
  import { useProject } from './useProject';
  import { createProject } from '/@/api/resource/project';
  import { BasicForm } from '/@/components/Form';
  import { PageWrapper } from '/@/components/Page';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { useMessage } from '/@/hooks/web/useMessage';
  const { getLoading, registerForm, submit, handleSubmit } = useProject();

  const { Swal, createMessage } = useMessage();
  const router = useRouter();
  const { t } = useI18n();

  /* Create project */
  async function handleCreateAction(values: Recordable) {
    try {
      const res = await createProject({
        name: values.name,
        url: values.url,
        repository: values.repository,
        type: values.type,
        refs: values.refs,
        userName: values.userName,
        password: values.password,
        prvkeyPath: values.prvkeyPath || null,
        pom: values.pom,
        buildArgs: values.buildArgs,
        description: values.description,
      });
      if (res.data) {
        createMessage.success('created successfully');
        router.go(-1);
      } else {
        Swal.fire('Failed', 'Project save failed ..>﹏<.. <br><br>' + res['message'], 'error');
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
      @submit="(values: Recordable) => handleSubmit(values, handleCreateAction)"
    >
      <template #formFooter>
        <div class="flex items-center w-full justify-center">
          <a-button @click="router.go(-1)">
            {{ t('common.cancelText') }}
          </a-button>
          <a-button
            id="e2e-project-submit-btn"
            class="ml-4"
            :loading="getLoading"
            type="primary"
            @click="submit()"
          >
            {{ t('common.submitText') }}
          </a-button>
        </div>
      </template>
    </BasicForm>
  </PageWrapper>
</template>
