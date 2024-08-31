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
  import { useI18n } from '/@/hooks/web/useI18n';
  import { onMounted, ref } from 'vue';
  import { useModal } from '/@/components/Modal';
  import { SvgIcon } from '/@/components/Icon';
  import { List, Switch, Card, Popconfirm, Tooltip } from 'ant-design-vue';
  import {
    CheckOutlined,
    CloseOutlined,
    DeleteOutlined,
    EditOutlined,
    PlusOutlined,
  } from '@ant-design/icons-vue';
  import { SparkEnvModal } from './components';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { PageWrapper } from '/@/components/Page';
  import { BasicTitle } from '/@/components/Basic';
  import { fetchSetDefault, fetchSparkEnvList, fetchSparkEnvRemove } from '/@/api/spark/home';
  import { SparkEnv } from '/@/api/spark/home.type';

  defineOptions({
    name: 'SparkEnvSetting',
  });

  const { t } = useI18n();
  const versionId = ref<string | null>(null);
  const { Swal, createMessage } = useMessage();
  const sparkEnvs = ref<SparkEnv[]>([]);
  const [registerModal, { openModal: openFlinkModal }] = useModal();
  /* Edit button */
  async function handleEditSpark(item: SparkEnv) {
    versionId.value = item.id;
    openFlinkModal(true, {
      versionId: item.id,
      sparkName: item.sparkName,
      sparkHome: item.sparkHome,
      description: item.description || null,
    });
  }

  /* delete spark home */
  async function handleDelete(item: SparkEnv) {
    await fetchSparkEnvRemove(item.id);
    await getSparkEnv();
    createMessage.success(t('spark.home.tips.remove'));
  }

  /* set as default environment */
  async function handleSetDefault(item: SparkEnv) {
    if (item.isDefault) {
      await fetchSetDefault(item.id);
      Swal.fire({
        icon: 'success',
        title: item.sparkName.concat(t('spark.home.tips.setDefault')),
        showConfirmButton: false,
        timer: 2000,
      });
      getSparkEnv();
    }
  }

  /* Get spark environment data */
  async function getSparkEnv() {
    sparkEnvs.value = await fetchSparkEnvList();
  }

  onMounted(() => {
    getSparkEnv();
  });
</script>
<template>
  <PageWrapper contentFullHeight content-background>
    <Card :bordered="false">
      <BasicTitle>{{ t('spark.home.title') }}</BasicTitle>
      <div>
        <a-button
          type="dashed"
          style="width: 100%; margin-top: 20px"
          @click="openFlinkModal(true, {})"
        >
          <PlusOutlined />
          {{ t('common.add') }}
        </a-button>
      </div>
      <List>
        <List.Item v-for="(item, index) in sparkEnvs" :key="index">
          <List.Item.Meta
            style="width: 60%"
            :title="item.sparkName"
            :description="item.description"
          >
            <template #avatar>
              <SvgIcon class="avatar p-15px" name="spark" size="60" />
            </template>
          </List.Item.Meta>

          <div class="list-content flex" style="width: 40%">
            <div class="list-content-item" style="width: 60%">
              <span>{{ t('spark.home.title') }}</span>
              <p style="margin-top: 10px">
                {{ item.sparkHome }}
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
            <Tooltip :title="t('common.edit')">
              <a-button
                @click="handleEditSpark(item)"
                shape="circle"
                size="large"
                class="control-button"
              >
                <EditOutlined />
              </a-button>
            </Tooltip>
            <Popconfirm
              :title="t('common.delText')"
              :cancel-text="t('common.no')"
              :ok-text="t('common.yes')"
              @confirm="handleDelete(item)"
            >
              <a-button
                :disabled="item.isDefault && sparkEnvs.length > 1"
                type="danger"
                shape="circle"
                size="large"
                class="control-button"
              >
                <DeleteOutlined />
              </a-button>
            </Popconfirm>
          </template>
        </List.Item>
      </List>
    </Card>

    <SparkEnvModal @register="registerModal" @reload="getSparkEnv" />
  </PageWrapper>
</template>
<style lang="less"></style>
