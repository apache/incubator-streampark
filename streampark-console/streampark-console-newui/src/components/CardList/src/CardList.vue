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
  <div class="p-2">
    <div class="p-4 mb-2 bg-white">
      <BasicForm @register="registerForm" />
    </div>
    <div class="p-2 bg-white">
      <List
        :grid="{ gutter: 5, xs: 1, sm: 2, md: 4, lg: 4, xl: 6, xxl: grid }"
        :data-source="data"
        :pagination="paginationProp"
      >
        <template #header>
          <div class="flex justify-end space-x-2"
            ><slot name="header"></slot>
            <Tooltip>
              <template #title>
                <div class="w-50">Display quantity per line</div
                ><Slider
                  id="slider"
                  v-bind="sliderProp"
                  v-model:value="grid"
                  @change="sliderChange"
              /></template>
              <Button><TableOutlined /></Button>
            </Tooltip>
            <Tooltip @click="fetch">
              <template #title>refresh</template>
              <Button><RedoOutlined /></Button>
            </Tooltip>
          </div>
        </template>
        <template #renderItem="{ item }">
          <ListItem>
            <Card>
              <template #title></template>
              <template #cover>
                <div :class="height">
                  <Image :src="item.imgs[0]" />
                </div>
              </template>
              <template #actions>
                <!--              <SettingOutlined key="setting" />-->
                <EditOutlined key="edit" />
                <Dropdown
                  :trigger="['hover']"
                  :dropMenuList="[
                    {
                      text: 'delete',
                      event: '1',
                      popConfirm: {
                        title: 'Whether to confirm the deletion',
                        confirm: handleDelete.bind(null, item.id),
                      },
                    },
                  ]"
                  popconfirm
                >
                  <EllipsisOutlined key="ellipsis" />
                </Dropdown>
              </template>

              <CardMeta>
                <template #title>
                  <TypographyText :content="item.name" :ellipsis="{ tooltip: item.address }" />
                </template>
                <template #avatar>
                  <Avatar :src="item.avatar" />
                </template>
                <template #description>{{ item.time }}</template>
              </CardMeta>
            </Card>
          </ListItem>
        </template>
      </List>
    </div>
  </div>
</template>
<script lang="ts" setup>
  import { computed, onMounted, ref } from 'vue';
  import {
    EditOutlined,
    EllipsisOutlined,
    RedoOutlined,
    TableOutlined,
  } from '@ant-design/icons-vue';
  import { List, Card, Image, Typography, Tooltip, Slider, Avatar } from 'ant-design-vue';
  import { Dropdown } from '/@/components/Dropdown';
  import { BasicForm, useForm } from '/@/components/Form';
  import { propTypes } from '/@/utils/propTypes';
  import { Button } from '/@/components/Button';
  import { isFunction } from '/@/utils/is';
  import { useSlider, grid } from './data';
  const ListItem = List.Item;
  const CardMeta = Card.Meta;
  const TypographyText = Typography.Text;
  // Get slider property
  const sliderProp = computed(() => useSlider(4));
  // Component receives parameters
  const props = defineProps({
    // Request API parameters
    params: propTypes.object.def({}),
    //api
    api: propTypes.func,
  });
  // expose internal methods
  const emit = defineEmits(['getMethod', 'delete']);
  // data
  const data = ref([]);
  // Switch the number of lines per line
  // cover image adaptive height
  // Modify pageSize and re-request data

  const height = computed(() => {
    return `h-${120 - grid.value * 6}`;
  });
  // form
  const [registerForm, { validate }] = useForm({
    schemas: [{ field: 'type', component: 'Input', label: '类型' }],
    labelWidth: 80,
    baseColProps: { span: 6 },
    actionColOptions: { span: 24 },
    autoSubmitOnEnter: true,
    submitFunc: handleSubmit,
  });
  // form submit
  async function handleSubmit() {
    const data = await validate();
    await fetch(data);
  }
  function sliderChange(n) {
    pageSize.value = n * 4;
    fetch();
  }

  // Automatically request and expose internal methods
  onMounted(() => {
    fetch();
    emit('getMethod', fetch);
  });

  async function fetch(p = {}) {
    const { api, params } = props;
    if (api && isFunction(api)) {
      const res = await api({ ...params, page: page.value, pageSize: pageSize.value, ...p });
      data.value = res.items;
      total.value = res.total;
    }
  }
  // Pagination related
  const page = ref(1);
  const pageSize = ref(36);
  const total = ref(0);
  const paginationProp = ref({
    showSizeChanger: false,
    showQuickJumper: true,
    pageSize,
    current: page,
    total,
    showTotal: (total) => `总 ${total} 条`,
    onChange: pageChange,
    onShowSizeChange: pageSizeChange,
  });

  function pageChange(p, pz) {
    page.value = p;
    pageSize.value = pz;
    fetch();
  }
  function pageSizeChange(_current, size) {
    pageSize.value = size;
    fetch();
  }

  async function handleDelete(id) {
    emit('delete', id);
  }
</script>
