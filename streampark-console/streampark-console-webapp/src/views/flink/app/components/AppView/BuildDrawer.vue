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
  import { defineComponent } from 'vue';
  export default defineComponent({
    name: 'BuildDrawer',
  });
</script>
<script setup lang="ts" name="BuildDrawer">
  import { reactive, ref, unref } from 'vue';
  import { Timeline, Empty, Divider, Tag, Progress, List, Space } from 'ant-design-vue';
  import { BasicDrawer, useDrawer, useDrawerInner } from '/@/components/Drawer';
  import { Icon } from '/@/components/Icon';
  import {
    handleAppBuildStatusColor,
    handleAppBuildStatueText,
    handleAppBuildStepTimelineColor,
    handleAppBuildStepText,
  } from '../../utils';
  import BuildLayer from './BuildLayer.vue';
  import { AppListRecord } from '/@/api/flink/app.type';
  import { useTimeoutFn } from '@vueuse/core';
  import { fetchBuildDetail } from '/@/api/flink/flinkBuild';
  import { useI18n } from '/@/hooks/web/useI18n';

  const TimelineItem = Timeline.Item;
  const ListItem = List.Item;

  const { t } = useI18n();
  const appId = ref<Nullable<string>>(null);
  const appBuildDetail = reactive<{ pipeline: Nullable<any>; docker: Nullable<any> }>({
    pipeline: null,
    docker: null,
  });

  const { isPending, start, stop } = useTimeoutFn(
    () => {
      handleFetchBuildDetail();
    },
    1000,
    { immediate: false },
  );
  /* Build Detail */
  async function handleFetchBuildDetail() {
    try {
      if (unref(appId) == null) {
        console.warn('appId is null, skipping fetching build detail');
        return;
      }
      const res = await fetchBuildDetail({
        appId: unref(appId)!,
      });
      Object.assign(appBuildDetail, res);
      start();
    } catch (error) {
      console.error('error', error);
      stop();
    }
  }

  const [registerBuild] = useDrawerInner((data: { app: AppListRecord }) => {
    data && onReceiveData(data);
  });
  /* data reception */
  function onReceiveData(data: Recordable) {
    appId.value = data.appId;
    if (!isPending.value) start();
  }

  const [registerErrorLog, { openDrawer: openErrorDrawer }] = useDrawer();
</script>
<template>
  <BasicDrawer
    @register="registerBuild"
    :title="t('flink.app.view.buildTitle')"
    width="500"
    :closable="true"
    @close="stop()"
  >
    <!-- status and cost time -->
    <h3>
      <Icon icon="ant-design:dashboard-outlined" />
      Summary
    </h3>
    <template v-if="appBuildDetail.pipeline">
      <div>
        <Progress
          v-if="appBuildDetail.pipeline.hasError"
          :percent="appBuildDetail.pipeline.percent"
          status="exception"
        />
        <Progress
          v-else-if="appBuildDetail.pipeline.percent < 100"
          :percent="appBuildDetail.pipeline.percent"
          status="active"
        />
        <Progress v-else :percent="appBuildDetail.pipeline.percent" />
      </div>
      <div class="mt-10px">
        <template v-if="appBuildDetail.pipeline.pipeStatus == 2">
          <Tag
            :color="handleAppBuildStatusColor(appBuildDetail.pipeline.pipeStatus)"
            class="running-tag"
          >
            {{ handleAppBuildStatueText(appBuildDetail.pipeline.pipeStatus) }}
          </Tag>
        </template>
        <template v-else>
          <Tag :color="handleAppBuildStatusColor(appBuildDetail.pipeline.pipeStatus)">
            {{ handleAppBuildStatueText(appBuildDetail.pipeline.pipeStatus) }}
          </Tag>
        </template>
        cost {{ appBuildDetail.pipeline.costSec }} seconds
      </div>
    </template>
    <Empty v-else />
    <Divider />

    <!-- step detail -->
    <h3>
      <Icon icon="ant-design:project-outlined" />

      {{ t('flink.app.view.stepTitle') }}
    </h3>
    <template v-if="appBuildDetail.pipeline">
      <Timeline style="margin-top: 20px">
        <TimelineItem
          v-for="stepItem in appBuildDetail.pipeline.steps"
          :key="stepItem.seq"
          :color="handleAppBuildStepTimelineColor(stepItem)"
        >
          <!-- step status, desc -->
          <p>
            <template v-if="stepItem.status == 2">
              <Tag :color="handleAppBuildStepTimelineColor(stepItem)" class="running-tag">
                {{ handleAppBuildStepText(stepItem.status) }}
              </Tag>
              <b>Step-{{ stepItem.seq }}</b> {{ stepItem.desc }}
            </template>
            <template v-else>
              <Tag :color="handleAppBuildStepTimelineColor(stepItem)">
                {{ handleAppBuildStepText(stepItem.status) }}
              </Tag>
              <b>Step-{{ stepItem.seq }}</b> {{ stepItem.desc }}
            </template>
          </p>
          <!-- step info update time --->
          <template v-if="stepItem.status !== 0 && stepItem.status !== 1">
            <p style="color: gray; font-size: 12px">{{ stepItem.ts }}</p>
          </template>
          <!-- docker resolved detail --->
          <template v-if="appBuildDetail.pipeline.pipeType === 2 && appBuildDetail.docker !== null">
            <template
              v-if="
                stepItem.seq === 5 &&
                appBuildDetail.docker.pull !== null &&
                appBuildDetail.docker.pull.layers !== null
              "
            >
              <template v-for="layer in appBuildDetail.docker.pull.layers" :key="layer.layerId">
                <BuildLayer :layer="layer" />
              </template>
            </template>

            <template
              v-else-if="
                stepItem.seq === 6 &&
                appBuildDetail.docker.build !== null &&
                appBuildDetail.docker.build.steps != null
              "
            >
              <List bordered :data-source="appBuildDetail.docker.build.steps" size="small">
                <template #renderItem="{ item }">
                  <ListItem>
                    <Space>
                      <Icon icon="ant-design:arrow-right-outlined" />
                      <span style="font-size: 12px">{{ item }}</span>
                    </Space>
                  </ListItem>
                </template>
              </List>
            </template>

            <template
              v-else-if="
                stepItem.seq === 7 &&
                appBuildDetail.docker.push !== null &&
                appBuildDetail.docker.push.layers !== null
              "
            >
              <template v-for="layer in appBuildDetail.docker.push.layers" :key="layer.layerId">
                <BuildLayer :layer="layer" />
              </template>
            </template>
          </template>
        </TimelineItem>
      </Timeline>
    </template>
    <Empty v-else />

    <!-- bottom tools -->
    <template v-if="appBuildDetail.pipeline != null && appBuildDetail.pipeline.hasError">
      <div
        class="absolute -bottom-0 -left-0 w-full px-16px py-10px text-right bg-white rounded-bl-4px"
        style="border-top: 1px solid #e8e8e8"
      >
        <a-button type="primary" @click="openErrorDrawer(true)">
          <Icon icon="ant-design:warning-outlined" />
          {{ t('flink.app.view.errorLog') }}
        </a-button>
      </div>
      <BasicDrawer
        @register="registerErrorLog"
        :title="t('flink.app.view.errorLog')"
        width="800"
        :closable="true"
      >
        <div>
          <h3>{{ t('flink.app.view.errorSummary') }}</h3>
          <br />
          <p>{{ appBuildDetail.pipeline.errorSummary }}</p>
          <Divider />
          <h3>{{ t('flink.app.view.errorStack') }}</h3>
          <br />
          <pre style="font-size: 12px">{{ appBuildDetail.pipeline.errorStack }}</pre>
        </div>
      </BasicDrawer>
    </template>
  </BasicDrawer>
</template>
