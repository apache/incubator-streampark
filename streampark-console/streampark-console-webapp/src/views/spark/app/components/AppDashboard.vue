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
  import { onMounted, reactive, ref } from 'vue';
  import StatisticCard from './StatisticCard.vue';
  import { Row, Col } from 'ant-design-vue';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { fetchSparkDashboard } from '/@/api/spark/app';
  const dashBigScreenMap = reactive<Recordable>({});
  const dashboardLoading = ref(true);
  const { t } = useI18n();

  // Get Dashboard Metrics Data
  async function handleDashboard(showLoading: boolean) {
    try {
      dashboardLoading.value = showLoading;
      const res = await fetchSparkDashboard();
      if (res) {
        Object.assign(dashBigScreenMap, {
          runningTask: {
            statistics: {
              title: t('spark.app.dashboard.runningTasks'),
              value: res.runningApplication,
            },
            footer: [
              { title: t('spark.app.dashboard.totalTask'), value: res?.numTasks || 0 },
              { title: t('spark.app.dashboard.totalStage'), value: res?.numStages || 0 },
            ],
          },
          completedTask: {
            statistics: {
              title: t('spark.app.dashboard.completedTask'),
              value: res.numCompletedTasks,
            },
            footer: [
              { title: t('spark.app.dashboard.completedStage'), value: res.numCompletedStages },
            ],
          },
          memory: {
            statistics: {
              title: t('spark.app.dashboard.memory'),
              value: `${res.usedMemory} MB`,
            },
            footer: [],
          },
          taskManager: {
            statistics: {
              title: t('spark.app.dashboard.VCore'),
              value: res.usedVCores,
            },
            footer: [],
          },
        });
      }
    } catch (error) {
      console.error(error);
    } finally {
      dashboardLoading.value = false;
    }
  }

  onMounted(() => {
    handleDashboard(true);
  });

  defineExpose({ handleDashboard });
</script>
<template>
  <Row :gutter="24">
    <Col class="gutter-row" :md="6" :xs="24" v-for="(value, key) in dashBigScreenMap" :key="key">
      <StatisticCard
        :statisticProps="value.statistics"
        :footerList="value.footer"
        :loading="dashboardLoading"
      />
    </Col>
  </Row>
</template>
