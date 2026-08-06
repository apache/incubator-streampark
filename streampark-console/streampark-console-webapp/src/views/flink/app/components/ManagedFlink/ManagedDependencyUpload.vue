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
<script setup lang="ts">
  import { computed, ref } from 'vue';
  import { Button, Select, Upload } from 'ant-design-vue';
  import type { UploadRequestOption } from 'ant-design-vue/es/vc-upload/interface';
  import { checkResource, fetchAddResource, fetchUpload } from '/@/api/resource/upload';
  import type { ResourceListRecord } from '/@/api/resource/upload/model/resourceModel';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useI18n } from '/@/hooks/web/useI18n';

  const props = defineProps<{
    value?: string[];
    resources: ResourceListRecord[];
  }>();

  const emit = defineEmits<{
    (event: 'update:value', value: string[]): void;
    (event: 'uploaded', resourceName: string): void;
  }>();

  const { t } = useI18n();
  const { createMessage } = useMessage();
  const uploading = ref(false);

  const options = computed(() =>
    props.resources
      .filter(
        (resource) => resource.engineType === 'FLINK' && resource.resourceType === 'JAR_LIBRARY',
      )
      .map((resource) => ({ label: resource.resourceName, value: resource.resourceName })),
  );

  function beforeUpload(file: File) {
    if (!file.name.toLowerCase().endsWith('.jar')) {
      createMessage.error(t('flink.app.managed.dependencyJarOnly'));
      return Upload.LIST_IGNORE;
    }
    if (props.resources.some((resource) => resource.resourceName === file.name)) {
      createMessage.error(t('flink.app.managed.dependencyAlreadyExists'));
      return Upload.LIST_IGNORE;
    }
    return true;
  }

  async function uploadDependency(option: UploadRequestOption) {
    const file = option.file as File;
    uploading.value = true;
    try {
      const formData = new FormData();
      formData.append('file', file);
      const uploaded = await fetchUpload(formData);
      const resource = JSON.stringify({ jar: [`${file.name}:${uploaded.path}`] });
      const resourceParam = {
        resourceName: file.name,
        resourceType: 'JAR_LIBRARY',
        engineType: 'FLINK',
        description: t('flink.app.managed.dependencyUploadDescription'),
        resource,
      };
      const checked = await checkResource(resourceParam);
      if (checked['state'] !== 0) {
        throw new Error(t('flink.app.managed.dependencyUploadInvalid'));
      }
      await fetchAddResource(resourceParam);
      const next = Array.from(new Set([...(props.value || []), file.name]));
      emit('update:value', next);
      emit('uploaded', file.name);
      option.onSuccess?.(uploaded);
      createMessage.success(t('flink.app.managed.dependencyUploadSuccess'));
    } catch (error) {
      option.onError?.(error as Error);
      if (error instanceof Error) createMessage.error(error.message);
    } finally {
      uploading.value = false;
    }
  }
</script>

<template>
  <div class="flex w-full gap-2">
    <Select
      class="flex-1"
      mode="multiple"
      :value="value || []"
      :options="options"
      :max-tag-count="3"
      show-search
      option-filter-prop="label"
      :placeholder="t('flink.app.managed.dependencyPlaceholder')"
      @update:value="emit('update:value', $event)"
    />
    <Upload
      accept=".jar"
      :show-upload-list="false"
      :before-upload="beforeUpload"
      :custom-request="uploadDependency"
    >
      <Button :loading="uploading">{{ t('flink.app.managed.uploadDependency') }}</Button>
    </Upload>
  </div>
</template>
