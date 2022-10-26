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
  import { fetchDashboard } from '/@/api/flink/app/app';
  import StatisticCard from './statisticCard.vue';
  import { Row, Col } from 'ant-design-vue';
  const dashBigScreenMap = reactive<Recordable>({});
  const dashboardLoading = ref(true);

  // Get Dashboard Metrics Data
  async function handleDashboard(showLoading: boolean) {
    try {
      dashboardLoading.value = showLoading;
      const res = await fetchDashboard();
      if (res) {
        Object.assign(dashBigScreenMap, {
          availiableTask: {
            staticstics: { title: 'Available Task Slots', value: res.availableSlot },
            footer: [
              { title: 'Task Slots', value: res.totalSlot },
              { title: 'Task Managers', value: res.totalTM },
            ],
          },
          runningJob: {
            staticstics: { title: 'Running Jobs', value: res.runningJob },
            footer: [
              { title: 'Total Task', value: res.task.total },
              { title: 'Running Task', value: res.task.running },
            ],
          },
          jobManager: {
            staticstics: { title: 'JobManager Memory', value: res.jmMemory },
            footer: [{ title: 'Total JobManager Mem', value: `${res.jmMemory} MB` }],
          },
          taskManager: {
            staticstics: { title: 'TaskManager Memory', value: res.tmMemory },
            footer: [{ title: 'Total TaskManager Mem', value: `${res.tmMemory} MB` }],
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
  <Row :gutter="24" class="dashboard">
    <Col
      class="gutter-row mt-10px"
      :md="6"
      :xs="24"
      v-for="(value, key) in dashBigScreenMap"
      :key="key"
    >
      <StatisticCard
        :statisticProps="value.staticstics"
        :footerList="value.footer"
        :loading="dashboardLoading"
      />
    </Col>
  </Row>
</template>
<style lang="less"></style>
