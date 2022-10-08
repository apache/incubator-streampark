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
          <Card :bordered="false" class="system-setting">
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
