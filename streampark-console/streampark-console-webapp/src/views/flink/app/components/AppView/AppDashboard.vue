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
  import { fetchDashboard } from '/@/api/flink/app';
  import StatisticCard from './StatisticCard.vue';
  import { Row, Col } from 'ant-design-vue';
  import { useI18n } from '/@/hooks/web/useI18n';
  const dashBigScreenMap = reactive<Recordable>({});
  const dashboardLoading = ref(true);
  const { t } = useI18n();

  // Get Dashboard Metrics Data
  async function handleDashboard(showLoading: boolean) {
    try {
      dashboardLoading.value = showLoading;
      const res = await fetchDashboard();
      if (res) {
        Object.assign(dashBigScreenMap, {
          runningJob: {
            staticstics: { title: t('flink.app.dashboard.runningJobs'), value: res.runningJob },
            footer: [
              { title: t('flink.app.dashboard.totalTask'), value: res.task.total },
              { title: t('flink.app.dashboard.runningTask'), value: res.task.running },
            ],
          },
          availiableTask: {
            staticstics: {
              title: t('flink.app.dashboard.availableTaskSlots'),
              value: res.availableSlot,
            },
            footer: [
              { title: t('flink.app.dashboard.taskSlots'), value: res.totalSlot },
              { title: t('flink.app.dashboard.taskManagers'), value: res.totalTM },
            ],
          },
          jobManager: {
            staticstics: { title: t('flink.app.dashboard.jobManagerMemory'), value: res.jmMemory },
            footer: [
              {
                title: t('flink.app.dashboard.totalJobManagerMemory'),
                value: `${res.jmMemory} MB`,
              },
            ],
          },
          taskManager: {
            staticstics: { title: t('flink.app.dashboard.taskManagerMemory'), value: res.tmMemory },
            footer: [
              {
                title: t('flink.app.dashboard.totalTaskManagerMemory'),
                value: `${res.tmMemory} MB`,
              },
            ],
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
        :statisticProps="value.staticstics"
        :footerList="value.footer"
        :loading="dashboardLoading"
      />
    </Col>
  </Row>
</template>
<style lang="less"></style>
