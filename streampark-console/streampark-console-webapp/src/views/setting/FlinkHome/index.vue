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
<script lang="ts" setup name="FlinkEnvSetting">
  import { onMounted, ref } from 'vue';
  import { useModal } from '/@/components/Modal';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { Icon, SvgIcon } from '/@/components/Icon';
  import { List, Switch, Popconfirm, Tooltip } from 'ant-design-vue';
  import {
    CheckOutlined,
    CloseOutlined,
    DeleteOutlined,
    EyeOutlined,
    PlusOutlined,
  } from '@ant-design/icons-vue';
  import { FlinkEnvModal, FlinkEnvDrawer } from './components';
  import {
    fetchValidity,
    fetchDefaultSet,
    fetchFlinkEnv,
    fetchFlinkEnvRemove,
    fetchFlinkInfo,
  } from '/@/api/flink/setting/flinkEnv';
  import { FlinkEnv } from '/@/api/flink/setting/types/flinkEnv.type';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useDrawer } from '/@/components/Drawer';
  import { PageWrapper } from '/@/components/Page';
  import { BasicTitle } from '/@/components/Basic';
  defineOptions({
    name: 'FlinkEnvSetting',
  });
  const ListItem = List.Item;
  const ListItemMeta = ListItem.Meta;

  const { t } = useI18n();
  const versionId = ref<string | null>(null);
  const { Swal, createMessage } = useMessage();
  const flinks = ref<FlinkEnv[]>([]);
  const [registerModal, { openModal: openFlinkModal }] = useModal();
  const [registerFlinkDraw, { openDrawer: openEnvDrawer }] = useDrawer();
  /* Edit button */
  async function handleEditFlink(item: FlinkEnv) {
    const resp = await fetchValidity(item.id);
    if (resp.data.code == 200) {
      versionId.value = item.id;
      openFlinkModal(true, {
        versionId: item.id,
        flinkName: item.flinkName,
        flinkHome: item.flinkHome,
        description: item.description || null,
      });
    }
  }

  /* View configuration */
  async function handleFlinkConf(item: FlinkEnv) {
    const res = await fetchFlinkInfo(item.id);
    openEnvDrawer(true, res);
  }

  /* delete flink home */
  async function handleDelete(item: FlinkEnv) {
    const resp = await fetchFlinkEnvRemove(item.id);
    if (resp.data.code == 200) {
      await getFlinkSetting();
      createMessage.success('The current flink home is removed.');
    }
  }

  /* set as default environment */
  async function handleSetDefault(item: FlinkEnv) {
    if (item.isDefault) {
      await fetchDefaultSet(item.id);
      Swal.fire({
        icon: 'success',
        title: item.flinkName.concat(' set default successful!'),
        showConfirmButton: false,
        timer: 2000,
      });
      getFlinkSetting();
    }
  }

  /* Get flink environment data */
  async function getFlinkSetting() {
    flinks.value = await fetchFlinkEnv();
  }

  onMounted(() => {
    getFlinkSetting();
  });
</script>
<template>
  <PageWrapper contentFullHeight fixed-height content-class="flex flex-col">
    <div class="bg-white py-16px px-24px">
      <BasicTitle class="!inline-block" style="margin: 0 !important; height: initial">
        {{ t('setting.flinkHome.title') }}
      </BasicTitle>
      <div v-auth="'project:create'">
        <a-button type="dashed" class="w-full mt-10px" @click="openFlinkModal(true, {})">
          <PlusOutlined />
          {{ t('common.add') }}
        </a-button>
      </div>
    </div>
    <div class="flex-1">
      <List class="home-card-list !mt-10px">
        <ListItem v-for="(item, index) in flinks" :key="index">
          <ListItemMeta style="width: 60%" :title="item.flinkName" :description="item.description">
            <template #avatar>
              <SvgIcon class="avatar p-15px" name="flink" size="60" />
            </template>
          </ListItemMeta>

          <div class="list-content flex" style="width: 40%">
            <div class="list-content-item" style="width: 60%">
              <span>Flink Home</span>
              <p style="margin-top: 10px">
                {{ item.flinkHome }}
              </p>
            </div>
            <div class="list-content-item">
              <span>Default</span>
              <p style="margin-top: 10px">
                <Switch
                  :disabled="item.isDefault"
                  @click="handleSetDefault(item)"
                  v-model:checked="item.isDefault"
                >
                  <template #checkedChildren>
                    <CheckOutlined />
                  </template>
                  <template #unCheckedChildren>
                    <CloseOutlined />
                  </template>
                </Switch>
              </p>
            </div>
          </div>

          <template #actions>
            <Tooltip :title="t('setting.flinkHome.edit')">
              <a-button
                @click="handleEditFlink(item)"
                shape="circle"
                size="large"
                class="control-button"
              >
                <Icon icon="clarity:note-edit-line" />
              </a-button>
            </Tooltip>
            <Tooltip :title="t('setting.flinkHome.conf')">
              <a-button
                shape="circle"
                @click="handleFlinkConf(item)"
                target="_blank"
                size="large"
                class="control-button"
              >
                <EyeOutlined />
              </a-button>
            </Tooltip>
            <Popconfirm
              :title="t('setting.flinkHome.delete')"
              :cancel-text="t('common.no')"
              :ok-text="t('common.yes')"
              @confirm="handleDelete(item)"
            >
              <a-button
                :disabled="item.isDefault && flinks.length > 1"
                type="danger"
                shape="circle"
                size="large"
                class="control-button"
              >
                <DeleteOutlined />
              </a-button>
            </Popconfirm>
          </template>
        </ListItem>
      </List>
    </div>

    <FlinkEnvModal @register="registerModal" @reload="getFlinkSetting" />
    <FlinkEnvDrawer @register="registerFlinkDraw" width="60%" />
  </PageWrapper>
</template>
<style lang="less" scoped>
  .home-card-list {
    background-color: @component-background;
    height: 100%;
  }
</style>
