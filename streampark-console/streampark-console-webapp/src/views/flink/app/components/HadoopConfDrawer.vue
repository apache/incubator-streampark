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
<script lang="ts" name="HadoopConfDrawer">
  import { defineComponent } from 'vue';
  import { useI18n } from '/@/hooks/web/useI18n';
  export default defineComponent({
    name: 'HadoopConfDrawer',
  });
</script>

<script setup lang="ts" name="HadoopConfDrawer">
  import { Tabs, Empty } from 'ant-design-vue';
  import { computed, reactive } from 'vue';
  import { fetchSysHadoopConf } from '/@/api/flink/config';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';

  const TabPane = Tabs.TabPane;

  const { t } = useI18n();
  const [registerHadoopConf] = useDrawerInner(() => onReceiveDrawerData());
  const hadoopConfContent = reactive<Recordable>({});

  const isHadoopEmpty = computed(
    () => !hadoopConfContent?.hadoop || Object.keys(hadoopConfContent.hadoop).length === 0,
  );
  const isHiveEmpty = computed(
    () => !hadoopConfContent?.hive || Object.keys(hadoopConfContent.hive).length === 0,
  );

  async function onReceiveDrawerData() {
    if (Object.keys(hadoopConfContent).length === 0) {
      const res = await fetchSysHadoopConf();
      Object.assign(hadoopConfContent, res);
    }
  }
</script>
<template>
  <BasicDrawer
    @register="registerHadoopConf"
    :title="t('flink.app.hadoopConfigTitle')"
    :width="800"
    item-layout="vertical"
  >
    <Tabs tabPosition="top">
      <TabPane key="hadoop" tab="Hadoop">
        <Empty v-if="isHadoopEmpty" />
        <template v-else>
          <Tabs tabPosition="left">
            <TabPane v-for="(content, fname) in hadoopConfContent.hadoop" :key="fname" :tab="fname">
              <pre class="my-20px" style="font-size: 12px">{{ content }}</pre>
            </TabPane>
          </Tabs>
        </template>
      </TabPane>
      <TabPane key="hive" tab="Hive">
        <Empty v-if="isHiveEmpty" />
        <template v-else>
          <Tabs tabPosition="left">
            <TabPane v-for="(content, fname) in hadoopConfContent.hive" :key="fname" :tab="fname">
              <pre class="my-20px" style="font-size: 12px">{{ content }}</pre>
            </TabPane>
          </Tabs>
        </template>
      </TabPane>
    </Tabs>
  </BasicDrawer>
</template>
