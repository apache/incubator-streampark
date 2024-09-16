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
    name: 'DependApp',
  };
</script>
<script setup lang="ts" name="DependApp">
  import { useRoute, useRouter } from 'vue-router';
  import { fetchDependApps } from '/@/api/resource/variable';
  import Icon from '/@/components/Icon';
  import { PageWrapper } from '/@/components/Page';
  import { BasicTable, useTable } from '/@/components/Table';
  import { useI18n } from '/@/hooks/web/useI18n';
  const { t } = useI18n();
  const route = useRoute();
  const router = useRouter();
  const [registerTable] = useTable({
    api: fetchDependApps,
    canResize: false,
    showIndexColumn: false,
    showTableSetting: false,
    tableSetting: { setting: true },
    beforeFetch(params: Recordable) {
      Object.assign(params, {
        variableCode: route.query.id,
      });
      return params;
    },
    columns: [
      { title: t('flink.variable.depend.jobName'), dataIndex: 'jobName', width: 500 },
      { title: t('flink.variable.depend.nickName'), dataIndex: 'nickName' },
      { title: t('common.createTime'), dataIndex: 'createTime' },
    ],
  });
</script>

<template>
  <PageWrapper content-full-height fixed-height class="flex flex-col">
    <div class="mb-15px py-24px px-10px bg-white">
      <a-button
        type="primary"
        shape="circle"
        @click="router.back()"
        class="float-right mr-10px -mt-8px"
      >
        <Icon icon="ant-design:arrow-left-outlined" />
      </a-button>
      <span class="app-bar">
        {{ t('flink.variable.depend.headerTitle', [route.query.id]) }}
      </span>
    </div>
    <BasicTable @register="registerTable" class="flex flex-col" />
  </PageWrapper>
</template>
