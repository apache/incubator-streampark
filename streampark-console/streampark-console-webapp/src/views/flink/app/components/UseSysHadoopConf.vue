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
  import { Switch, Space, Popover } from 'ant-design-vue';
  import { defineComponent } from 'vue';
  import { useDrawer } from '/@/components/Drawer';
  import Icon from '/@/components/Icon';
  import { useI18n } from '/@/hooks/web/useI18n';

  export default defineComponent({
    name: 'UseSysHadoopConf',
  });
</script>

<script setup lang="ts" name="UseSysHadoopConf">
  import HadoopConfDrawer from './HadoopConfDrawer.vue';
  const { t } = useI18n();
  defineProps({
    hadoopConf: {
      type: Boolean,
      required: true,
    },
  });
  const emit = defineEmits(['update:hadoopConf']);
  const [registerHadoopConf, { openDrawer: openHadoopConfDrawer }] = useDrawer();
</script>

<template>
  <div class="flex items-center">
    <Switch
      checked-children="ON"
      un-checked-children="OFF"
      :checked="hadoopConf"
      @change="(checked) => emit('update:hadoopConf', checked)"
    />
    <Space>
      <Popover title="Tips">
        <template #content>
          <p>Automatically copy configuration files from system environment parameters</p>
          <p><b>HADOOP_CONF_PATH</b> and <b>HIVE_CONF_PATH</b> to Flink Docker image</p>
        </template>
        <Icon icon="ant-design:question-circle-outlined" class="ml-10px" />
      </Popover>
      <transition name="slide-fade">
        <a-button size="small" v-if="hadoopConf" @click="openHadoopConfDrawer(true, {})">
          <div class="flex items-center">
            <Icon icon="ant-design:eye-outlined" class="pr-5px" />

            {{ t('common.view') }}
          </div>
        </a-button>
      </transition>
    </Space>
  </div>
  <HadoopConfDrawer @register="registerHadoopConf" />
</template>
