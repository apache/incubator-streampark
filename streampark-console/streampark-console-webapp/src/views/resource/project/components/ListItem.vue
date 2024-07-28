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
  <a-list-item>
    <a-list-item-meta class="item-meta">
      <template #title>
        <a-badge status="processing" title="installing" class="mr-10px" v-if="isBuilding" />
        <span>{{ item.name }}</span>
      </template>
      <template #description>
        <a-popover arrow-point-at-center trigger="hover" :content="item.url">
          <a-button class="desc-btn" target="_blank" :href="item.url">
            {{ item.description }}
          </a-button>
        </a-popover>
      </template>
      <template #avatar>
        <a-badge
          class="build-badge"
          v-if="needBuild"
          count="NEW"
          title="this project has changed, need rebuild"
        >
          <svg-icon class="avatar" :name="svgName" size="large" />
        </a-badge>
        <svg-icon v-else class="avatar" :name="svgName" size="large" />
      </template>
    </a-list-item-meta>

    <ul class="list-content">
      <li class="list-content_item">
        <span>{{ t('flink.project.form.cvs') }}</span>
        <p><github-outlined /></p>
      </li>
      <li class="list-content_item">
        <span>{{ t('flink.project.form.branches') }}</span>
        <p>
          <Tag color="blue" style="border-radius: 4px">{{ item.branches }}</Tag>
        </p>
      </li>
      <li class="list-content_item build_time">
        <span>{{ t('flink.project.form.lastBuild') }}</span>
        <p>{{ item.lastBuild || '--' }}</p>
      </li>
      <li class="list-content_item build_state">
        <span>{{ t('flink.project.form.buildState') }}</span>
        <p>
          <a-tag :color="buildState.color" :class="tagClass">{{ buildState.label }}</a-tag>
        </p>
      </li>
    </ul>
    <div class="operation">
      <a-tooltip :title="t('flink.project.operationTips.seeBuildLog')">
        <a-button
          shape="circle"
          @click="handleSeeLog"
          class="!leading-26px"
          v-auth="'project:build'"
        >
          <Icon icon="ant-design:code-outlined" />
        </a-button>
      </a-tooltip>

      <template v-if="!isBuilding">
        <a-tooltip :title="t('flink.project.operationTips.buildProject')">
          <a-popconfirm
            :title="t('flink.project.operationTips.buildProjectMessage')"
            :cancel-text="t('common.cancelText')"
            :ok-text="t('common.okText')"
            @confirm="handleBuild"
          >
            <a-button shape="circle" class="ml-8px" v-auth="'project:build'">
              <ThunderboltOutlined />
            </a-button>
          </a-popconfirm>
        </a-tooltip>
        <a-tooltip :title="t('flink.project.operationTips.updateProject')">
          <a-button v-auth="'project:update'" @click="handleEdit" shape="circle" class="ml-8px">
            <EditOutlined />
          </a-button>
        </a-tooltip>
      </template>

      <a-tooltip :title="t('flink.project.operationTips.deleteProject')">
        <a-popconfirm
          :title="t('flink.project.operationTips.deleteProjectMessage')"
          :cancel-text="t('common.cancelText')"
          :ok-text="t('common.okText')"
          @confirm="handleDelete"
        >
          <a-button type="danger" v-auth="'project:delete'" shape="circle" style="margin-left: 8px">
            <DeleteOutlined />
          </a-button>
        </a-popconfirm>
      </a-tooltip>
    </div>
  </a-list-item>
</template>
<script lang="ts" setup>
  import { List, Popover, Badge, Tag, Tooltip, Popconfirm } from 'ant-design-vue';
  import Icon, { SvgIcon } from '/@/components/Icon';
  import { buildUUID } from '/@/utils/uuid';
  import {
    GithubOutlined,
    DeleteOutlined,
    EditOutlined,
    ThunderboltOutlined,
  } from '@ant-design/icons-vue';
  import { buildStateMap } from '../project.data';
  import { computed } from 'vue';
  import { buildProject, deleteProject } from '/@/api/resource/project';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { ProjectRecord } from '/@/api/resource/project/model/projectModel';
  import { BuildStateEnum } from '/@/enums/flinkEnum';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { ProjectTypeEnum } from '/@/enums/projectEnum';
  import { router } from '/@/router';

  const { t } = useI18n();
  const emit = defineEmits(['viewLog', 'success']);

  const { Swal, createMessage } = useMessage();
  const props = defineProps({
    item: { type: Object as PropType<ProjectRecord>, required: true },
  });
  const needBuild = computed(() => props.item.buildState == BuildStateEnum.NEED_REBUILD);
  const isBuilding = computed(() => props.item.buildState == BuildStateEnum.BUILDING);
  const buildState = computed(() => {
    return buildStateMap[props.item.buildState] || buildStateMap[BuildStateEnum.FAILED];
  });
  const tagClass = computed(() => buildState.value.className || '');
  const svgName = computed(() => {
    return (
      {
        [ProjectTypeEnum.FLINK]: 'flink',
        [ProjectTypeEnum.SPARK]: 'spark',
      }[props.item.type] || ''
    );
  });

  async function handleBuild() {
    try {
      await buildProject({
        id: props.item.id,
        socketId: buildUUID(),
      });
      Swal.fire({
        icon: 'success',
        title: t('flink.project.operationTips.projectIsbuildingMessage'),
        showConfirmButton: false,
        timer: 2000,
      });
    } catch (e) {
      createMessage.error(t('flink.project.operationTips.projectIsbuildFailedMessage'));
    }
  }

  const handleEdit = function () {
    router.push({ path: '/project/edit', query: { id: props.item.id } });
  };

  async function handleDelete() {
    try {
      const resp = await deleteProject({ id: props.item.id });
      if (resp.data) {
        Swal.fire({
          icon: 'success',
          title: t('flink.project.operationTips.deleteProjectSuccessMessage'),
          showConfirmButton: false,
          timer: 2000,
        });
        emit('success', true);
      } else {
        Swal.fire(
          'Failed',
          t('flink.project.operationTips.deleteProjectFailedDetailMessage'),
          'error',
        );
      }
    } catch (e) {
      createMessage.error(t('flink.project.operationTips.deleteProjectFailedMessage'));
    }
  }

  function handleSeeLog() {
    emit('viewLog', props.item);
  }

  const AListItem = List.Item;
  const AListItemMeta = List.Item.Meta;
  const ABadge = Badge;
  const ATag = Tag;
  const ATooltip = Tooltip;
  const APopover = Popover;
  const APopconfirm = Popconfirm;
</script>

<style lang="less" scoped>
  .avatar {
    width: 50px;
    height: 50px;
    background: #eee;
    border-radius: 100%;
    padding: 10px;
  }

  .list-content {
    &_item {
      display: inline-block;
      vertical-align: middle;
      font-size: 14px;
      margin-left: 40px;

      &.build_time {
        width: 180px;
      }

      &.build_state {
        width: 150px;
      }

      span {
        line-height: 20px;
      }

      p {
        margin-top: 4px;
        margin-bottom: 0;
        line-height: 22px;
      }
    }

    .operation {
      width: 120px;

      .ant-btn-circle {
        margin: 0 5px;
      }
    }
  }

  .item-meta {
    .build-badge {
      font-size: 12px;
      transform: scale(0.84, 0.84);
    }

    .desc-btn {
      border: unset;
      height: 20px;
      background: unset;
      margin-left: 0;
      padding-left: 0;
    }
  }

  .ant-tag {
    border-radius: 0;
    font-weight: 700;
    text-align: center;
    padding: 0 4px;
    margin-right: 0;
    cursor: default;
  }
</style>
