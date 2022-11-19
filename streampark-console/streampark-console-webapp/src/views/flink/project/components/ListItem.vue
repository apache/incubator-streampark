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
        <span>CVS</span>
        <p><github-outlined /></p>
      </li>
      <li class="list-content_item">
        <span>Branches</span>
        <p>
          <a-tag color="blue">{{ item.branches }}</a-tag>
        </p>
      </li>
      <li class="list-content_item build_time">
        <span>Last Build</span>
        <p>{{ item.lastBuild || '--' }}</p>
      </li>
      <li class="list-content_item build_state">
        <span>Build State</span>
        <p>
          <a-tag :color="buildState.color" :class="tagClass">{{ buildState.label }}</a-tag>
        </p>
      </li>
    </ul>
    <div class="operation">
      <a-tooltip title="See Build log">
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
        <a-tooltip title="Build Project">
          <a-popconfirm
            title="Are you sure build this project?"
            cancel-text="No"
            ok-text="Yes"
            @confirm="handleBuild"
          >
            <a-button shape="circle" class="ml-8px" v-auth="'project:build'">
              <ThunderboltOutlined />
            </a-button>
          </a-popconfirm>
        </a-tooltip>
      </template>

      <a-tooltip title="Update Project">
        <a-button v-auth="'project:update'" @click="handleEdit" shape="circle" class="ml-8px">
          <EditOutlined />
        </a-button>
      </a-tooltip>
      <a-tooltip title="Delete Project">
        <a-popconfirm
          title="Are you sure delete this project ?"
          cancel-text="No"
          ok-text="Yes"
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
  import { ProjectType, buildStateMap } from '../project.data';
  import { computed } from 'vue';
  import { buildProject, deleteProject } from '/@/api/flink/project';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useGo } from '/@/hooks/web/usePage';
  import { ProjectRecord } from '/@/api/flink/project/model/projectModel';
  import { BuildStateEnum } from '/@/enums/flinkEnum';

  const emit = defineEmits(['viewLog', 'success']);

  const { Swal, createMessage } = useMessage();
  const go = useGo();
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
        [ProjectType.Flink]: 'flink',
        [ProjectType.Spark]: 'spark',
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
        title: 'The current project is building',
        showConfirmButton: false,
        timer: 2000,
      });
    } catch (e) {
      createMessage.error('Build Fail');
    }
  }

  const handleEdit = function () {
    go(`/flink/project/edit?id=${props.item.id}`);
  };

  async function handleDelete() {
    try {
      const { data } = await deleteProject({ id: props.item.id });
      if (data.data) {
        Swal.fire({
          icon: 'success',
          title: 'delete successful',
          showConfirmButton: false,
          timer: 2000,
        });
        emit('success', true);
      } else {
        Swal.fire('Failed', 'Please check if any application belongs to this project', 'error');
      }
    } catch (e) {
      createMessage.error('Delete Fail');
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
