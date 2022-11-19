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
  import { useI18n } from '/@/hooks/web/useI18n';
  export default defineComponent({
    name: 'MavenSetting',
  });
</script>
<script setup lang="ts" name="MavenSetting">
  import { List, Input, Switch } from 'ant-design-vue';
  import { SvgIcon } from '/@/components/Icon';
  import { SystemSetting } from '/@/api/flink/setting/types/setting.type';
  import { fetchSystemSettingUpdate } from '/@/api/flink/setting';
  import { useMessage } from '/@/hooks/web/useMessage';

  const AvatarMap = {
    'streampark.maven.settings': 'settings',
    'streampark.maven.central.repository': 'maven',
    'streampark.maven.auth.user': 'user',
    'streampark.maven.auth.password': 'mvnpass',
    'docker.register.address': 'docker',
    'docker.register.namespace': 'namespace',
    'docker.register.user': 'auth',
    'docker.register.password': 'password',
    'alert.email.host': 'host',
    'alert.email.port': 'port',
    'alert.email.from': 'mail',
    'alert.email.userName': 'user',
    'alert.email.password': 'keys',
    'alert.email.ssl': 'ssl',
    'streampark.console.webapp.address': 'http',
    'ingress.mode.default': 'settings',
  };

  const ListItem = List.Item;
  const ListItemMeta = ListItem.Meta;

  const { t } = useI18n();
  const emits = defineEmits(['updateValue', 'reload']);
  defineProps({
    data: {
      type: Array as PropType<SystemSetting[]>,
      required: true,
    },
    isPassword: {
      type: Function as PropType<(item: Recordable) => boolean>,
      required: true,
    },
  });

  const { createMessage } = useMessage();
  function handleSwitch(record: SystemSetting) {
    emits('updateValue', record);
  }
  /* edit input */
  function handleEdit(record: SystemSetting) {
    if (!record.editable) {
      record.submitting = true;
    }
    record.editable = !record.editable;
  }
  /* edit commit */
  async function handleSubmit(record: SystemSetting) {
    record.submitting = false;
    record.editable = false;
    await fetchSystemSettingUpdate({
      settingKey: record.settingKey,
      settingValue: record.settingValue,
    });
    createMessage.success('submit successfully');
    emits('reload');
  }
</script>

<template>
  <List>
    <template v-for="item in data" :key="item.settingKey">
      <ListItem>
        <ListItemMeta :title="item.settingName" :description="item.description" style="width: 50%">
          <template #avatar>
            <div class="avatar">
              <SvgIcon :name="AvatarMap[item.settingKey]" />
            </div>
          </template>
        </ListItemMeta>
        <div class="list-content" style="width: 50%">
          <div class="list-content-item" style="width: 100%">
            <template v-if="item.type === 1">
              <Input
                :type="isPassword(item) ? 'password' : 'text'"
                v-if="item.editable"
                v-model:value="item.settingValue"
                :class="item.settingKey.replace(/\./g, '_')"
                placeholder="Please enter"
                class="ant-input"
              />
              <div v-else style="width: 100%">
                <span v-if="isPassword(item) && item.settingValue !== null"> ******** </span>
                <span v-else>{{ item.settingValue }}</span>
              </div>
            </template>
            <template v-else>
              <Switch
                checked-children="ON"
                un-checked-children="OFF"
                style="float: right; margin-right: 30px"
                :checked="item.settingValue === 'true'"
                @change="handleSwitch(item)"
              />
            </template>
          </div>
        </div>
        <template #actions>
          <div v-if="item.type === 1">
            <a v-if="!item.submitting" @click="handleEdit(item)">
              {{ t('common.edit') }}
            </a>
            <a v-else @click="handleSubmit(item)">
              {{ t('common.submitText') }}
            </a>
          </div>
        </template>
      </ListItem>
    </template>
  </List>
</template>
