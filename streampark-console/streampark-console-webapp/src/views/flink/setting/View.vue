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
<template>
  <PageWrapper contentFullHeight>
    <div class="card-container">
      <Tabs type="card" class="setting" v-model:activeKey="activeKey">
        <TabPane tab="System Setting" key="system">
          <Card :bordered="false" class="system-setting">
            <SystemSetting />
          </Card>
        </TabPane>
        <TabPane tab="Alert Setting" key="alert">
          <Card
            :bordered="false"
            class="system-setting !bg-transparent"
            :body-style="{ padding: 0 }"
          >
            <AlertSetting />
          </Card>
        </TabPane>
        <TabPane tab="Flink Home" key="flink">
          <Card :bordered="false" class="system-setting">
            <FlinkEnvSetting />
          </Card>
        </TabPane>
        <TabPane tab="Flink Cluster" key="cluster">
          <Card :bordered="false" class="system-setting">
            <FlinkClusterSetting />
          </Card>
        </TabPane>
      </Tabs>
    </div>
  </PageWrapper>
</template>
<script lang="ts" setup>
  import { ref } from 'vue';
  import { Tabs, Card } from 'ant-design-vue';
  import { PageWrapper } from '/@/components/Page';
  import SystemSetting from './components/SystemSetting.vue';
  import AlertSetting from './components/AlertSetting.vue';
  import FlinkEnvSetting from './components/FlinkEnvSetting.vue';
  import FlinkClusterSetting from './components/FlinkClusterSetting.vue';
  import { useRoute } from 'vue-router';

  const TabPane = Tabs.TabPane;
  const route = useRoute();

  const activeKey = ref<string>((route?.query?.activeKey as string) || 'system');
</script>
<style lang="less">
  @import url('./View.less');
</style>
