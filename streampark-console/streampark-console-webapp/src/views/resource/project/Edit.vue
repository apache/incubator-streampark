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
    name: 'EditProject',
  });
</script>
<script setup lang="ts" name="EditProject">
  import { defineComponent, onMounted, ref } from 'vue';
  import { useRouter, useRoute } from 'vue-router';
  import { useProject } from './useProject';
  import { updateProject } from '/@/api/resource/project';
  import { BasicForm } from '/@/components/Form';
  import { PageWrapper } from '/@/components/Page';
  import { BuildStateEnum } from '/@/enums/flinkEnum';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { useMessage } from '/@/hooks/web/useMessage';
  const { getLoading, registerForm, submit, handleSubmit, handleGet, projectResource } =
    useProject();

  const { createErrorSwal, createMessage } = useMessage();
  const { t } = useI18n();
  const route = useRoute();
  const router = useRouter();

  const buildState = ref<Nullable<number | string> | undefined>(null);
  if (!route.query.id) {
    router.go(-1);
  }

  function handleCheckRebuild(values: Recordable) {
    if (
      projectResource.url !== values.url ||
      projectResource.branches !== values.branches ||
      projectResource.pom !== values.pom
    ) {
      buildState.value = BuildStateEnum.NEED_REBUILD;
    } else {
      buildState.value = projectResource.buildState;
    }
  }
  /* Update project */
  async function handleEditAction(values: Recordable) {
    try {
      handleCheckRebuild(values);
      const { data } = await updateProject({
        id: route.query.id,
        name: values.name,
        url: values.url,
        repository: values.repository,
        type: values.type,
        refs: values.refs,
        userName: values.userName,
        password: values.password,
        prvkeyPath: values.prvkeyPath,
        pom: values.pom,
        buildArgs: values.buildArgs,
        description: values.description,
        buildState: buildState.value,
      });
      if (data.data) {
        router.go(-1);
        createMessage.success('update successfully');
      } else {
        createErrorSwal('Project update failed ..>﹏<.. <br><br>' + data['message']);
      }
    } catch (error: any) {
      if (error?.data?.message) {
        createMessage.error('Project update failed:' + error.data['message']);
      } else {
        createMessage.error('Project update failed ..>﹏<.. ');
      }
    }
  }

  onMounted(() => {
    if (!route?.query?.id) {
      router.go(-1);
      createMessage.warning('appid can not be empty');
      return;
    }
    handleGet();
  });
</script>
<template>
  <PageWrapper contentFullHeight contentBackground contentClass="p-26px">
    <BasicForm
      @register="registerForm"
      @submit="(values: Recordable) => handleSubmit(values, handleEditAction)"
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
