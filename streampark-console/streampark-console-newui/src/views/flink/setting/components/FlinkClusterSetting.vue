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
    name: 'FlinkClusterSetting',
  });
</script>
<script lang="ts" setup name="FlinkClusterSetting">
  import { onMounted, ref, h } from 'vue';
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
  const { createMessage, createConfirm } = useMessage();
  const clusters = ref<FlinkCluster[]>([]);
  const optionClusters = {
    starting: new Map(),
    created: new Map(),
    stoped: new Map(),
  };
  /* 获取flink环境数据 */
  async function getFlinkClusterSetting() {
    const clusterList = await fetchFlinkCluster();
    clusters.value = clusterList;
    for (const key in clusterList) {
      const cluster = clusterList[key];
      if (cluster.clusterState === 0) {
        optionClusters.created.set(cluster.id, new Date().getTime());
      } else if (cluster.clusterState === 1) {
        optionClusters.starting.set(cluster.id, new Date().getTime());
      } else {
        optionClusters.stoped.set(cluster.id, new Date().getTime());
      }
    }
  }
  function handleAdd() {
    go('/flink/setting/add_cluster');
  }

  function handleIsStart(item) {
    /**
     The cluster was just created but not started
     CREATED(0),
     cluster started
     STARTED(1),
     cluster stopped
     STOPED(2);
    */
    return optionClusters.starting.get(item.id);
  }
  /* Go to edit cluster */
  function handleEditCluster(item: FlinkCluster) {
    go(`/flink/setting/edit_cluster?clusterId=${item.id}`);
  }
  /* deploy */
  async function handleDeployCluser(item: FlinkCluster) {
    const hide = createMessage.loading('The current cluster is starting', 0);
    try {
      const { data } = await fetchClusterStart(item.id);
      if (data?.data?.status) {
        optionClusters.starting.set(item.id, new Date().getTime());
        handleMapUpdate('starting');
        getFlinkClusterSetting();
        createMessage.success('The current cluster is started');
      } else {
        createConfirm({
          iconType: 'error',
          title: 'Failed',
          content: h(
            'div',
            { class: 'whitespace-pre-wrap', style: { maxHeight: '550px', overflow: 'auto' } },
            data?.data?.msg,
          ),
          width: '800px',
        });
      }
    } catch (error) {
      console.error(error);
    } finally {
      hide();
    }
  }
  /* delete */
  async function handleDelete(item: FlinkCluster) {
    const { data } = await fetchClusterRemove(item.id);
    if (data?.data?.status) {
      optionClusters.starting.delete(item.id);
      handleMapUpdate('starting');
      getFlinkClusterSetting();
      createMessage.success('The current cluster is remove');
    }
  }
  /* shutdown */
  async function handleShutdownCluster(item: FlinkCluster) {
    const hide = createMessage.loading('The current cluster is canceling', 0);
    try {
      const { data } = await fetchClusterShutdown(item.id);
      if (data?.data?.status) {
        optionClusters.starting.delete(item.id);
        handleMapUpdate('starting');
        createMessage.success('The current cluster is shutdown');
      } else {
        createConfirm({
          iconType: 'error',
          title: 'Failed',
          content: h('pre', { class: 'propsException' }, data?.data?.msg),
        });
      }
    } catch (error) {
      console.error(error);
    } finally {
      hide();
    }
  }

  function handleMapUpdate(type: string) {
    const map = optionClusters[type];
    optionClusters[type] = new Map(map);
  }

  onMounted(() => {
    getFlinkClusterSetting();
  });
</script>
<template>
  <div v-auth="'project:create'">
    <a-button type="dashed" style="width: 100%; margin-top: 20px" @click="handleAdd">
      <PlusOutlined />
      {{ t('common.add') }}
    </a-button>
  </div>
  <List>
    <ListItem v-for="(item, index) in clusters" :key="index">
      <ListItemMeta :title="item.clusterName" :description="item.description">
        <template #avatar>
          <SvgIcon class="avatar" name="flink" size="25" />
        </template>
      </ListItemMeta>
      <div class="list-content" style="width: 20%">
        <div class="list-content-item" style="width: 60%">
          <span>ExecutionMode</span>
          <p style="margin-top: 10px">
            {{ item.executionModeEnum.toLowerCase() }}
          </p>
        </div>
      </div>
      <div class="list-content" style="width: 20%">
        <div class="list-content-item" style="width: 80%">
          <span>ClusterId</span>
          <p style="margin-top: 10px">
            {{ item.clusterId }}
          </p>
        </div>
      </div>
      <div class="list-content" style="width: 30%">
        <div class="list-content-item" style="width: 60%">
          <span>Address</span>
          <p style="margin-top: 10px">
            {{ item.address }}
          </p>
        </div>
      </div>
      <template #actions>
        <Tooltip :title="t('flink.setting.cluster.edit')">
          <a-button
            v-if="handleIsStart(item) && item.executionMode === 3"
            v-auth="'app:update'"
            :disabled="true"
            @click="handleEditCluster(item)"
            shape="circle"
            size="large"
            style="margin-left: 3px"
            class="control-button ctl-btn-color"
          >
            <EditOutlined />
          </a-button>
          <a-button
            v-if="!handleIsStart(item) || item.executionMode === 1"
            v-auth="'app:update'"
            @click="handleEditCluster(item)"
            shape="circle"
            size="large"
            style="margin-left: 3px"
            class="control-button ctl-btn-color"
          >
            <EditOutlined />
          </a-button>
        </Tooltip>
        <template v-if="!handleIsStart(item)">
          <Tooltip :title="t('flink.setting.cluster.start')">
            <a-button
              v-if="item.executionMode === 3 || item.executionMode === 5"
              v-auth="'cluster:create'"
              @click="handleDeployCluser(item)"
              shape="circle"
              size="large"
              style="margin-left: 3px"
              class="control-button ctl-btn-color"
            >
              <PlayCircleOutlined />
            </a-button>
            <a-button
              v-else
              :disabled="true"
              v-auth="'cluster:create'"
              shape="circle"
              size="large"
              style="margin-left: 3px"
              class="control-button ctl-btn-color"
            >
              <PlayCircleOutlined />
            </a-button>
          </Tooltip>
        </template>

        <template v-else>
          <Tooltip :title="t('flink.setting.cluster.stop')">
            <a-button
              v-if="[3, 5].includes(item.executionMode)"
              v-auth="'cluster:create'"
              @click="handleShutdownCluster(item)"
              shape="circle"
              size="large"
              style="margin-left: 3px"
              class="control-button ctl-btn-color"
            >
              <PauseCircleOutlined />
            </a-button>
            <a-button
              v-else
              :disabled="true"
              v-auth="'cluster:create'"
              shape="circle"
              size="large"
              style="margin-left: 3px"
              class="control-button ctl-btn-color"
            >
              <PauseCircleOutlined />
            </a-button>
          </Tooltip>
        </template>

        <Tooltip :title="t('flink.setting.cluster.detail')">
          <a-button
            v-if="!handleIsStart(item)"
            v-auth="'app:detail'"
            :disabled="true"
            shape="circle"
            size="large"
            style="margin-left: 3px"
            class="control-button ctl-btn-color"
          >
            <EyeOutlined />
          </a-button>
          <a-button
            v-else
            v-auth="'app:detail'"
            shape="circle"
            size="large"
            style="margin-left: 3px"
            class="control-button ctl-btn-color"
            :href="item.address"
            target="_blank"
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
          <a-button
            type="danger"
            shape="circle"
            size="large"
            style="margin-left: 3px"
            class="control-button"
          >
            <DeleteOutlined />
          </a-button>
        </Popconfirm>
      </template>
    </ListItem>
  </List>
</template>
