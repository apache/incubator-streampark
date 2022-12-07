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
  import { ClusterStateEnum, ExecModeEnum } from '/@/enums/flinkEnum';
  export default defineComponent({
    name: 'FlinkClusterSetting',
  });
</script>
<script lang="ts" setup name="FlinkClusterSetting">
  import { onMounted, ref } from 'vue';
  import { SvgIcon } from '/@/components/Icon';
  import { List, Popconfirm, Tooltip } from 'ant-design-vue';
  import {
    PauseCircleOutlined,
    EyeOutlined,
    PlusOutlined,
    PlayCircleOutlined,
    EditOutlined,
    DeleteOutlined,
  } from '@ant-design/icons-vue';
  import { useMessage } from '/@/hooks/web/useMessage';
  import {
    fetchClusterRemove,
    fetchClusterShutdown,
    fetchClusterStart,
    fetchFlinkCluster,
  } from '/@/api/flink/setting/flinkCluster';
  import { FlinkCluster } from '/@/api/flink/setting/types/flinkCluster.type';
  import { useGo } from '/@/hooks/web/usePage';
  import { useI18n } from '/@/hooks/web/useI18n';

  const ListItem = List.Item;
  const ListItemMeta = ListItem.Meta;

  const go = useGo();
  const { t } = useI18n();
  const { Swal, createMessage } = useMessage();
  const clusters = ref<FlinkCluster[]>([]);

  function handleIsStart(item) {
    return item.clusterState === ClusterStateEnum.STARTED;
  }

  /* Go to edit cluster */
  function handleEditCluster(item: FlinkCluster) {
    go(`/flink/setting/edit_cluster?clusterId=${item.id}`);
  }
  /* deploy */
  async function handleDeployCluster(item: FlinkCluster) {
    const hide = createMessage.loading(
      t('flink.setting.cluster.operateMessage.flinkClusterIsStarting'),
      0,
    );
    try {
      await fetchClusterStart(item.id);
      await Swal.fire({
        icon: 'success',
        title: t('flink.setting.cluster.operateMessage.flinkClusterHasStartedSuccessful'),
        showConfirmButton: false,
        timer: 2000,
      });
    } catch (error) {
      console.error(error);
    } finally {
      hide();
    }
  }
  /* delete */
  async function handleDelete(item: FlinkCluster) {
    await fetchClusterRemove(item.id);
    await getFlinkCluster();
    createMessage.success('The current cluster is remove');
  }
  /* shutdown */
  async function handleShutdownCluster(item: FlinkCluster) {
    const hide = createMessage.loading('The current cluster is canceling', 0);
    try {
      await fetchClusterShutdown(item.id);
      createMessage.success('The current cluster is shutdown');
    } catch (error) {
      console.error(error);
    } finally {
      hide();
    }
  }

  async function getFlinkCluster() {
    const clusterList = await fetchFlinkCluster();
    clusters.value = clusterList;
  }

  onMounted(() => {
    getFlinkCluster();
    setInterval(() => getFlinkCluster(), 1000 * 3);
  });
</script>
<template>
  <div v-auth="'project:create'">
    <a-button
      type="dashed"
      style="width: 100%; margin-top: 20px"
      @click="go('/flink/setting/add_cluster')"
    >
      <PlusOutlined />
      {{ t('common.add') }}
    </a-button>
  </div>
  <List>
    <ListItem v-for="(item, index) in clusters" :key="index">
      <ListItemMeta :title="item.clusterName" :description="item.description">
        <template #avatar>
          <SvgIcon class="avatar p-15px" name="flink" size="60" />
        </template>
      </ListItemMeta>
      <div class="list-content" style="width: 15%">
        <div class="list-content-item" style="width: 60%">
          <span>{{ t('flink.setting.cluster.form.executionMode') }}</span>
          <p style="margin-top: 10px">
            {{ item.executionModeEnum.toLowerCase() }}
          </p>
        </div>
      </div>
      <div
        class="list-content"
        style="width: 40%"
        v-if="
          item.executionMode === ExecModeEnum.REMOTE ||
          item.executionMode === ExecModeEnum.YARN_SESSION
        "
      >
        <div class="list-content-item">
          <span>{{ t('flink.setting.cluster.form.address') }}</span>
          <p style="margin-top: 10px">
            {{ item.address }}
          </p>
        </div>
      </div>
      <template #actions>
        <Tooltip :title="t('flink.setting.cluster.edit')">
          <a-button
            v-auth="'cluster:update'"
            :disabled="handleIsStart(item)"
            @click="handleEditCluster(item)"
            shape="circle"
            size="large"
            class="control-button"
          >
            <EditOutlined />
          </a-button>
        </Tooltip>
        <template v-if="handleIsStart(item)">
          <Tooltip :title="t('flink.setting.cluster.stop')">
            <a-button
              :disabled="item.executionMode === ExecModeEnum.REMOTE"
              v-auth="'cluster:create'"
              @click="handleShutdownCluster(item)"
              shape="circle"
              size="large"
              style="margin-left: 3px"
              class="control-button"
            >
              <PauseCircleOutlined />
            </a-button>
          </Tooltip>
        </template>
        <template v-else>
          <Tooltip :title="t('flink.setting.cluster.start')">
            <a-button
              :disabled="item.executionMode === ExecModeEnum.REMOTE"
              v-auth="'cluster:create'"
              @click="handleDeployCluster(item)"
              shape="circle"
              size="large"
              class="control-button"
            >
              <PlayCircleOutlined />
            </a-button>
          </Tooltip>
        </template>
        <Tooltip :title="t('flink.setting.cluster.detail')">
          <a-button
            :disabled="!handleIsStart(item)"
            v-auth="'app:detail'"
            shape="circle"
            :href="item.address"
            target="_blank"
            size="large"
            class="control-button"
          >
            <EyeOutlined />
          </a-button>
        </Tooltip>

        <Popconfirm
          :title="t('flink.setting.cluster.delete')"
          :cancel-text="t('common.no')"
          :ok-text="t('common.yes')"
          @confirm="handleDelete(item)"
        >
          <a-button type="danger" shape="circle" size="large" class="control-button">
            <DeleteOutlined />
          </a-button>
        </Popconfirm>
      </template>
    </ListItem>
  </List>
</template>
