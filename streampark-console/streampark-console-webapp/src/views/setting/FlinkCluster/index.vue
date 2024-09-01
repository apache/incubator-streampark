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
<script lang="ts" setup name="FlinkClusterSetting">
  import { onUnmounted } from 'vue';
  import { useTimeoutFn } from '@vueuse/core';
  import { onMounted, ref } from 'vue';
  import { SvgIcon } from '/@/components/Icon';
  import { List, Popconfirm, Tooltip, Tag } from 'ant-design-vue';
  import { ClusterStateEnum, ExecModeEnum } from '/@/enums/flinkEnum';
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
  import { PageWrapper } from '/@/components/Page';
  import { BasicTitle } from '/@/components/Basic';
  defineOptions({
    name: 'FlinkClusterSetting',
  });
  const ListItem = List.Item;
  const ListItemMeta = ListItem.Meta;

  const go = useGo();
  const { t } = useI18n();
  const { Swal, createMessage } = useMessage();
  const clusters = ref<FlinkCluster[]>([]);
  const loading = ref(false);
  function handleIsStart(item) {
    return item.clusterState === ClusterStateEnum.STARTED;
  }

  /* Go to edit cluster */
  function handleEditCluster(item: FlinkCluster) {
    go(`/setting/edit_cluster?clusterId=${item.id}`);
  }
  /* deploy */
  async function handleDeployCluster(item: FlinkCluster) {
    const hide = createMessage.loading(
      t('setting.flinkCluster.operateMessage.flinkClusterIsStarting'),
      0,
    );
    try {
      await fetchClusterStart(item.id);
      await Swal.fire({
        icon: 'success',
        title: t('setting.flinkCluster.operateMessage.flinkClusterHasStartedSuccessful'),
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
    try {
      loading.value = true;
      clusters.value = await fetchFlinkCluster();
    } catch (error) {
      console.error(error);
    } finally {
      loading.value = false;
    }
  }
  const { start, stop } = useTimeoutFn(() => {
    // Prevent another request from being initiated while the previous request is pending
    if (!loading.value) {
      getFlinkCluster();
    }
    start();
  }, 1000 * 3);

  onMounted(() => {
    getFlinkCluster();
  });
  onUnmounted(() => {
    stop();
  });
</script>
<template>
  <PageWrapper contentFullHeight fixed-height content-class="flex flex-col">
    <div class="bg-white py-16px px-24px">
      <BasicTitle class="!inline-block" style="margin: 0 !important; height: initial">
        {{ t('setting.flinkCluster.title') }}
      </BasicTitle>
      <div v-auth="'project:create'">
        <a-button type="dashed" class="w-full mt-10px" @click="go('/setting/add_cluster')">
          <PlusOutlined />
          {{ t('common.add') }}
        </a-button>
      </div>
    </div>
    <div class="flex-1">
      <List class="cluster-card-list !mt-10px">
        <ListItem v-for="(item, index) in clusters" :key="index">
          <ListItemMeta
            :title="item.clusterName"
            style="width: 20%"
            :description="item.description"
          >
            <template #avatar>
              <SvgIcon class="avatar p-15px" name="flink" size="60" />
            </template>
          </ListItemMeta>
          <div class="list-content" style="width: 20%">
            <div class="list-content-item">
              <span>{{ t('setting.flinkCluster.form.executionMode') }}</span>
              <p style="margin-top: 10px" v-if="item.executionMode === ExecModeEnum.STANDALONE">
                <Tag color="#2db7f5">standalone</Tag>
              </p>
              <p
                style="margin-top: 10px"
                v-else-if="item.executionMode === ExecModeEnum.YARN_SESSION"
              >
                <Tag color="#87d068">yarn session</Tag>
              </p>
              <p
                style="margin-top: 10px"
                v-else-if="item.executionMode === ExecModeEnum.KUBERNETES_SESSION"
              >
                <Tag color="#108ee9">k8s session</Tag>
              </p>
            </div>
          </div>
          <div
            class="list-content"
            style="width: 35%"
            v-if="
              item.executionMode === ExecModeEnum.STANDALONE ||
              item.executionMode === ExecModeEnum.YARN_SESSION
            "
          >
            <div class="list-content-item">
              <span>{{ t('setting.flinkCluster.form.address') }}</span>
              <p style="margin-top: 10px">
                <a :href="`/proxy/cluster/${item.id}/`" target="_blank">
                  {{ item.address }}
                </a>
              </p>
            </div>
          </div>
          <template #actions>
            <Tooltip :title="t('setting.flinkCluster.edit')">
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
              <Tooltip :title="t('setting.flinkCluster.stop')">
                <a-button
                  :disabled="item.executionMode === ExecModeEnum.STANDALONE"
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
              <Tooltip :title="t('setting.flinkCluster.start')">
                <a-button
                  :disabled="item.executionMode === ExecModeEnum.STANDALONE"
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
            <Tooltip :title="t('setting.flinkCluster.detail')">
              <a-button
                :disabled="!handleIsStart(item)"
                v-auth="'app:detail'"
                shape="circle"
                :href="`/proxy/cluster/${item.id}/`"
                target="_blank"
                size="large"
                class="control-button"
              >
                <EyeOutlined />
              </a-button>
            </Tooltip>

            <Popconfirm
              :title="t('setting.flinkCluster.delete')"
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
    </div>
  </PageWrapper>
</template>
<style lang="less" scoped>
  .cluster-card-list {
    background-color: @component-background;
    height: 100%;
  }
</style>
