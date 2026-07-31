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
  import { fetchManagedApplicationStatistics } from '/@/api/flink/managedFlink';
  import type { ManagedFlinkApplicationStatistics } from '/@/api/flink/managedFlink.type';
  import StatisticCard from './StatisticCard.vue';
  import { Row, Col } from 'ant-design-vue';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { useUserStoreWithOut } from '/@/store/modules/user';
  const dashBigScreenMap = reactive<Recordable>({});
  const managedDashboardMap = reactive<Recordable>({});
  const dashboardLoading = ref(true);
  const { t } = useI18n();
  const userStore = useUserStoreWithOut();

  // Get Dashboard Metrics Data
  async function handleDashboard(showLoading: boolean) {
    try {
      dashboardLoading.value = showLoading;
      const teamId = userStore.getTeamId;
      const [res, managed] = await Promise.all([
        fetchDashboard(),
        teamId
          ? fetchManagedApplicationStatistics({ teamId }).catch(() => undefined)
          : Promise.resolve(undefined),
      ]);
      if (res) {
        const hasManaged = toNumber(managed?.total) > 0;
        const unavailableWhenManaged = (value: number) => (hasManaged && value === 0 ? '—' : value);
        Object.assign(dashBigScreenMap, {
          runningJob: {
            staticstics: {
              title: t('flink.app.dashboard.runningJobs'),
              value: toNumber(res.runningJob) + toNumber(managed?.running),
            },
            footer: [
              {
                title: t('flink.app.dashboard.totalTask'),
                value: unavailableWhenManaged(res.task.total),
              },
              {
                title: t('flink.app.dashboard.runningTask'),
                value: unavailableWhenManaged(res.task.running),
              },
            ],
          },
          availiableTask: {
            staticstics: {
              title: t('flink.app.dashboard.availableTaskSlots'),
              value: unavailableWhenManaged(res.availableSlot),
            },
            footer: [
              {
                title: t('flink.app.dashboard.taskSlots'),
                value: unavailableWhenManaged(res.totalSlot),
              },
              {
                title: t('flink.app.dashboard.taskManagers'),
                value: unavailableWhenManaged(res.totalTM),
              },
            ],
          },
          jobManager: {
            staticstics: {
              title: t('flink.app.dashboard.jobManagerMemory'),
              value: unavailableWhenManaged(res.jmMemory),
            },
            footer: [
              {
                title: t('flink.app.dashboard.totalJobManagerMemory'),
                value: res.jmMemory === 0 && hasManaged ? '—' : `${res.jmMemory} MB`,
              },
            ],
          },
          taskManager: {
            staticstics: {
              title: t('flink.app.dashboard.taskManagerMemory'),
              value: unavailableWhenManaged(res.tmMemory),
            },
            footer: [
              {
                title: t('flink.app.dashboard.totalTaskManagerMemory'),
                value: res.tmMemory === 0 && hasManaged ? '—' : `${res.tmMemory} MB`,
              },
            ],
          },
        });
        renderManagedDashboard(managed);
      }
    } catch (error) {
      console.error(error);
    } finally {
      dashboardLoading.value = false;
    }
  }

  function toNumber(value?: number | string) {
    const normalized = Number(value || 0);
    return Number.isFinite(normalized) ? normalized : 0;
  }

  function renderManagedDashboard(managed?: ManagedFlinkApplicationStatistics) {
    Object.keys(managedDashboardMap).forEach((key) => delete managedDashboardMap[key]);
    const normalized = {
      total: toNumber(managed?.total),
      running: toNumber(managed?.running),
      healthy: toNumber(managed?.healthy),
      degraded: toNumber(managed?.degraded),
      notFound: toNumber(managed?.notFound),
      drifted: toNumber(managed?.drifted),
      pending: toNumber(managed?.pending),
    };
    if (normalized.total === 0) {
      return;
    }
    Object.assign(managedDashboardMap, {
      total: {
        staticstics: {
          title: t('flink.app.managed.statistics.total'),
          value: normalized.total,
        },
        footer: [{ title: t('flink.app.managed.statistics.running'), value: normalized.running }],
      },
      healthy: {
        staticstics: {
          title: t('flink.app.managed.statistics.healthy'),
          value: normalized.healthy,
        },
        footer: [{ title: t('flink.app.managed.statistics.pending'), value: normalized.pending }],
      },
      degraded: {
        staticstics: {
          title: t('flink.app.managed.statistics.degraded'),
          value: normalized.degraded,
        },
        footer: [{ title: t('flink.app.managed.statistics.notFound'), value: normalized.notFound }],
      },
      drifted: {
        staticstics: {
          title: t('flink.app.managed.statistics.drifted'),
          value: normalized.drifted,
        },
        footer: [
          {
            title: t('flink.app.managed.statistics.attention'),
            value: normalized.degraded + normalized.notFound + normalized.drifted,
          },
        ],
      },
    });
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
  <Row v-if="Object.keys(managedDashboardMap).length" :gutter="24" class="mt-16px">
    <Col class="gutter-row" :md="6" :xs="24" v-for="(value, key) in managedDashboardMap" :key="key">
      <StatisticCard
        :statisticProps="value.staticstics"
        :footerList="value.footer"
        :loading="dashboardLoading"
      />
    </Col>
  </Row>
</template>
<style lang="less"></style>
