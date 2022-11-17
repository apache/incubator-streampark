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
  export default {
    name: 'UploadJobJar',
  };
</script>
<script setup lang="ts" name="UploadJobJar">
  import { Upload } from 'ant-design-vue';
  import { Icon } from '/@/components/Icon';
  import { UploadRequestOption } from 'ant-design-vue/es/vc-upload/interface';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useI18n } from '/@/hooks/web/useI18n';

  const UploadDragger = Upload.Dragger;

  const { t } = useI18n();
  const { createMessage } = useMessage();
  const emit = defineEmits(['update:loading']);
  defineProps({
    customRequest: {
      type: Function as PropType<(item: UploadRequestOption) => Promise<any>>,
      require: true,
    },
    loading: {
      type: Boolean,
      default: false,
    },
  });
  function handleUploadJar(info) {
    const status = info.file.status;
    if (status === 'done') {
      emit('update:loading', false);
    } else if (status === 'error') {
      emit('update:loading', false);
      createMessage.error(`${info.file.name} file upload failed.`);
    }
  }
  /* Callback before file upload */
  function handleBeforeUpload(file) {
    if (file.type !== 'application/java-archive') {
      if (!/\.(jar|JAR)$/.test(file.name)) {
        emit('update:loading', false);
        createMessage.error('Only jar files can be uploaded! please check your file.');
        return false;
      }
    }
    emit('update:loading', true);
    return true;
  }
</script>

<template>
  <div>
    <UploadDragger
      name="file"
      :multiple="true"
      @change="handleUploadJar"
      :showUploadList="loading"
      :customRequest="customRequest"
      :beforeUpload="handleBeforeUpload"
    >
      <div class="h-266px">
        <p class="ant-upload-drag-icon !pt-40px">
          <Icon icon="ant-design:inbox-outlined" :style="{ fontSize: '70px' }" />
        </p>
        <p class="ant-upload-text h-45px">
          {{ t('flink.app.dragUploadTitle') }}
        </p>
        <p class="ant-upload-hint h-45px">
          {{ t('flink.app.dragUploadTip') }}
        </p>
      </div>
    </UploadDragger>
    <slot name="uploadInfo"></slot>
  </div>
</template>
